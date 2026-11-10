const assert = require('node:assert/strict')
const path = require('node:path')
const os = require('node:os')

let playwright
try {
  playwright = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
} catch (error) {
  if (process.env.PLAYWRIGHT_MODULE) throw error
  playwright = require(path.join(os.homedir(), '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright'))
}

// Authentication and read-only dialogs only. Every other non-GET request is blocked.
const base = process.env.ADMIN_UI_URL || 'http://127.0.0.1:3001'
const output = path.resolve(__dirname, '../test-results')
const deepLink = '/adoptions?status=0'
const checks = []
const runtimeErrors = []
const blockedWrites = []
const pass = name => { checks.push(name); console.log(`PASS ${name}`) }
const adminName = process.env.ADMIN_UI_USER || 'admin'
const adminPassword = process.env.ADMIN_UI_PASSWORD || '123456'

async function run() {
  const browser = await playwright.chromium.launch({
    executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe',
    headless: true
  })
  let page
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    await context.route('**/*', async route => {
      const req = route.request()
      const url = new URL(req.url())
      const login = req.method() === 'POST' && url.origin === new URL(base).origin && url.pathname === '/api/auth/login'
      if (req.method() === 'GET' || login) return route.continue()
      blockedWrites.push({ method: req.method(), path: url.pathname })
      return route.fulfill({ status: 400, contentType: 'application/json', body: JSON.stringify({ code: 400, message: '只读检查已阻止业务写入' }) })
    })
    page = await context.newPage()
    page.setDefaultTimeout(15000)
    page.on('pageerror', error => runtimeErrors.push(error.message))
    page.on('console', message => {
      if (message.type() === 'error' && /\[admin-ui\]|Failed to resolve|Uncaught/.test(message.text())) runtimeErrors.push(message.text())
    })
    const healthy = async () => {
      assert.equal(await page.locator('.fatal-error').count(), 0, 'No fatal screen')
      assert.deepEqual(runtimeErrors, [], 'No uncaught runtime errors')
    }
    const settled = async () => {
      await page.waitForLoadState('networkidle')
      await page.locator('.session-status').waitFor({ state: 'hidden' })
      await healthy()
    }
    const capture = async name => {
      await page.screenshot({ path: path.join(output, `admin-refined-${name}.png`), fullPage: true, animations: 'disabled', style: '.el-message { visibility: hidden !important; }' })
    }
    const username = () => page.getByRole('textbox', { name: '管理员账号', exact: true })
    const password = () => page.getByLabel('登录密码', { exact: true })
    const submit = async (name, secret, enter = false) => {
      await username().fill(name)
      await password().fill(secret)
      const responsePromise = page.waitForResponse(response => response.request().method() === 'POST' && new URL(response.url()).pathname === '/api/auth/login')
      if (enter) await password().press('Enter')
      else await page.getByRole('button', { name: '登录管理工作台', exact: true }).click()
      return (await responsePromise).json()
    }
    const assertLoginRedirect = () => {
      const url = new URL(page.url())
      assert.equal(url.pathname, '/login')
      assert.equal(url.searchParams.get('redirect'), deepLink)
    }
    const assertCacheSanitized = async () => {
      const result = await page.evaluate(() => {
        const value = JSON.parse(localStorage.getItem('adminUser') || 'null')
        return { hasToken: Boolean(localStorage.getItem('token')), role: value?.role, containsToken: Boolean(value && Object.hasOwn(value, 'token')), containsPassword: Boolean(value && Object.hasOwn(value, 'password')) }
      })
      assert.deepEqual(result, { hasToken: true, role: 1, containsToken: false, containsPassword: false })
    }

    await page.goto(base + deepLink)
    await page.waitForURL(url => url.pathname === '/login')
    assertLoginRedirect()
    await settled()
    await capture('login')
    pass('Unauthenticated deep link preserves redirect')

    await page.getByRole('button', { name: '登录管理工作台', exact: true }).click()
    await page.locator('.el-form-item__error').first().waitFor()
    assert.equal(await page.locator('.el-form-item__error').count(), 2)
    pass('Empty credentials produce two required-field errors')

    const incorrectPassword = 'readonly-check-wrong-password'
    const invalid = await submit(adminName, incorrectPassword)
    assert.notEqual(invalid.code, 200, 'Wrong password must be rejected by the real backend')
    await page.locator('.login-feedback.error').waitFor()
    await page.waitForTimeout(800)
    assert.ok(await page.locator('.login-feedback.error').isVisible(), 'Error persists instead of disappearing with a toast')
    assert.equal(await username().inputValue(), adminName)
    assert.equal(await password().inputValue(), incorrectPassword)
    assertLoginRedirect()
    await capture('login-error')
    pass('Real wrong-password error persists and retains inputs')

    const regular = await submit(process.env.ADMIN_REGULAR_USER || 'user1', process.env.ADMIN_REGULAR_PASSWORD || '123456')
    assert.equal(regular.code, 200, 'Existing ordinary user can authenticate')
    assert.equal(regular.data.role, 0)
    await page.locator('.login-feedback.error').waitFor()
    assert.match(await page.locator('.login-feedback.error').innerText(), /没有管理员权限/)
    assert.equal(await page.evaluate(() => Boolean(localStorage.getItem('token') || localStorage.getItem('adminUser'))), false)
    assertLoginRedirect()
    pass('Real ordinary account is refused without caching a session')

    const admin = await submit(adminName, adminPassword, true)
    assert.equal(admin.code, 200)
    assert.equal(admin.data.role, 1)
    await page.waitForURL(url => url.pathname === '/adoptions' && url.searchParams.get('status') === '0')
    await settled()
    await assertCacheSanitized()
    pass('Admin Enter login returns to deep link and sanitizes cached user')

    await page.getByRole('button', { name: '管理员账号菜单', exact: true }).click()
    await page.getByRole('menuitem', { name: '管理员资料', exact: true }).click()
    const profile = page.getByRole('dialog', { name: '管理员资料', exact: true })
    await profile.waitFor()
    assert.match(await profile.innerText(), /用户名/)
    assert.match(await profile.innerText(), /昵称/)
    assert.ok((await profile.locator('.detail-fields dd').allTextContents()).some(text => text.includes(adminName)), 'Administrator identity is shown')
    assert.equal(await profile.locator('input, textarea').count(), 0, 'Profile dialog is read-only')
    await capture('admin-profile')
    await profile.getByRole('button', { name: '关闭资料', exact: true }).click()
    await profile.waitFor({ state: 'hidden' })
    pass('Administrator profile is readable and read-only')

    await page.getByRole('button', { name: '工作台使用指南', exact: true }).click()
    const guide = page.getByRole('dialog', { name: '工作台使用指南', exact: true })
    await guide.waitFor()
    const entries = await guide.locator('.guide-grid a').evaluateAll(links => links.map(link => ({ path: new URL(link.href).pathname, title: link.querySelector('strong')?.textContent.trim(), description: link.querySelector('p')?.textContent.trim() })))
    assert.deepEqual(entries.map(item => item.path).sort(), ['/dashboard', '/users', '/pets', '/adoptions', '/health', '/vaccine', '/announcements'].sort())
    assert.ok(entries.every(item => item.title && item.description?.length > 8))
    await capture('guide')
    await guide.locator('a[href="/pets"]').click()
    await page.waitForURL(url => url.pathname === '/pets')
    await guide.waitFor({ state: 'hidden' })
    await settled()
    pass('Guide exposes seven real destinations and its link navigates')

    await page.setViewportSize({ width: 390, height: 844 })
    await page.getByRole('button', { name: '工作台使用指南', exact: true }).click()
    await guide.waitFor()
    const bounds = await guide.boundingBox()
    assert.ok(bounds && bounds.x >= 0 && bounds.x + bounds.width <= 390, 'Guide fits the mobile viewport')
    const mobileGuide = await guide.evaluate(element => ({ width: element.clientWidth, scrollWidth: element.scrollWidth, count: element.querySelectorAll('.guide-grid a').length }))
    assert.ok(mobileGuide.scrollWidth <= mobileGuide.width + 1)
    assert.equal(mobileGuide.count, 7)
    await capture('guide-mobile')
    await page.keyboard.press('Escape')
    await guide.waitFor({ state: 'hidden' })
    pass('Mobile guide fits its viewport and closes with Escape')

    await page.setViewportSize({ width: 1440, height: 1000 })
    await page.evaluate(() => localStorage.setItem('token', 'expired-session-readonly-check'))
    await page.goto(base + deepLink)
    await page.waitForURL(url => url.pathname === '/login')
    await settled()
    assertLoginRedirect()
    assert.equal(await page.evaluate(() => Boolean(localStorage.getItem('token') || localStorage.getItem('adminUser'))), false)
    pass('Expired token clears cache and preserves deep link through Layout')

    await submit(adminName, adminPassword, true)
    await page.waitForURL(url => url.pathname === '/adoptions' && url.searchParams.get('status') === '0')
    await settled()
    await assertCacheSanitized()
    pass('Reauthentication after expiry returns to the original deep link')

    await page.getByRole('button', { name: '管理员账号菜单', exact: true }).click()
    await page.getByRole('menuitem', { name: '退出登录', exact: true }).click()
    await page.waitForURL(url => url.pathname === '/login')
    await settled()
    assert.equal(await page.evaluate(() => Boolean(localStorage.getItem('token') || localStorage.getItem('adminUser'))), false)
    await page.setViewportSize({ width: 390, height: 844 })
    const pageSize = await page.evaluate(() => ({ width: innerWidth, scrollWidth: document.documentElement.scrollWidth }))
    assert.ok(pageSize.scrollWidth <= pageSize.width + 1)
    await capture('login-mobile')
    pass('Explicit logout clears cache; mobile login has no horizontal overflow')

    assert.deepEqual(blockedWrites, [], 'No unexpected non-GET business operations were attempted')
    await healthy()
    console.log(JSON.stringify({ passed: checks.length, runtimeErrors, blockedWrites, screenshots: ['admin-refined-login.png', 'admin-refined-login-error.png', 'admin-refined-admin-profile.png', 'admin-refined-guide.png', 'admin-refined-guide-mobile.png', 'admin-refined-login-mobile.png'] }, null, 2))
  } catch (error) {
    if (page && !page.isClosed()) await page.screenshot({ path: path.join(output, 'admin-refined-auth-failure.png'), fullPage: true }).catch(() => {})
    throw error
  } finally {
    await browser.close()
  }
}

run().catch(error => { console.error(error.message); process.exitCode = 1 })
