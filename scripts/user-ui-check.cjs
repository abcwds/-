const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert = require('node:assert/strict')
const path = require('node:path')
const base = process.env.USER_UI_URL || 'http://127.0.0.1:3000'
const screenshotDir = path.resolve(__dirname, '../test-results')
const checks = []
const pass = message => { checks.push(message); console.log(`PASS ${message}`) }

async function run() {
  const browser = await chromium.launch({ executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true })
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    const page = await context.newPage()
    const errors = []
    let recordRequests = 0
    page.on('request', request => { if (/\/api\/(health|vaccine)\/pet\//.test(request.url())) recordRequests++ })
    page.on('pageerror', error => errors.push(error.message))
    const healthy = async () => { assert.deepEqual(errors, []); assert.equal(await page.locator('vite-error-overlay').count(), 0) }
    const settle = async () => { await page.waitForLoadState('networkidle'); await healthy() }
    const shot = async name => {
      await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }))
      await page.screenshot({ path: path.join(screenshotDir, `user-refined-${name}.png`), fullPage: true, animations: 'disabled', style: '.el-message { visibility: hidden !important; }' })
    }
    const visit = async route => { await page.goto(base + route); await settle() }
    const noOverflow = async () => assert.ok(await page.evaluate(() => document.body.scrollWidth <= innerWidth + 1), 'No page horizontal overflow')
    const username = process.env.USER_UI_USERNAME || 'user1'
    const password = process.env.USER_UI_PASSWORD || '123456'
    const login = await context.request.post(base + '/api/auth/login', { data: { username, password } })
    const session = (await login.json()).data
    assert.ok(session?.token)

    await visit('/profile')
    assert.equal(new URL(page.url()).pathname, '/login')
    assert.equal(new URL(page.url()).searchParams.get('redirect'), '/profile')
    await shot('login')
    await page.locator('.auth-submit').click()
    await page.locator('.el-form-item__error').first().waitFor()
    await healthy()
    await page.getByLabel('用户名', { exact: true }).fill(username)
    await page.getByLabel('登录密码', { exact: true }).fill('wrong-password-for-ui-check')
    await page.locator('.auth-submit').click()
    await page.locator('.auth-error').waitFor()
    assert.equal(await page.getByLabel('用户名', { exact: true }).inputValue(), username)
    pass('Protected-page redirect, login validation and persistent server error')

    await page.getByRole('link', { name: '创建一个新账号' }).click()
    await settle()
    assert.equal(new URL(page.url()).searchParams.get('redirect'), '/profile')
    await shot('register')
    await page.getByRole('button', { name: '创建账号，开启陪伴' }).click()
    await page.locator('.el-form-item__error').first().waitFor()
    assert.equal(await page.locator('.el-form-item__error').count(), 6)
    await page.getByLabel('用户名', { exact: true }).fill(username)
    await page.getByLabel('你的昵称', { exact: true }).fill('界面检查')
    await page.getByLabel('手机号', { exact: true }).fill('13900139000')
    await page.getByLabel('电子邮箱', { exact: true }).fill('ui-check@example.com')
    await page.getByLabel('设置密码', { exact: true }).fill(password)
    await page.getByLabel('确认密码', { exact: true }).fill('mismatching-password')
    await page.getByRole('button', { name: '创建账号，开启陪伴' }).click()
    await page.getByText('两次输入的密码不一致', { exact: true }).waitFor()
    await page.getByLabel('确认密码', { exact: true }).fill(password)
    await page.getByRole('button', { name: '创建账号，开启陪伴' }).click()
    await page.locator('.auth-error').waitFor()
    pass('Registration required fields, password match and duplicate account rejection')

    // Intercept successful registration to verify the full UI flow without creating an account.
    let registrationPayload
    await page.route('**/api/auth/register', async route => {
      registrationPayload = route.request().postDataJSON()
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data: session, message: '界面检查：未创建账号' }) })
    })
    await page.getByRole('button', { name: '创建账号，开启陪伴' }).click()
    await page.waitForURL('**/profile')
    await settle()
    assert.equal(recordRequests, 0, 'Personal overview does not eagerly load every pet health record')
    assert.deepEqual(Object.keys(registrationPayload).sort(), ['email', 'nickname', 'password', 'phone', 'username'])
    await page.locator('.identity-card').waitFor()
    await shot('profile')
    assert.equal(await page.locator('.account-button').count(), 1)
    pass('Successful registration UI, return destination and immediate header sign-in state')

    const redirects = await page.evaluate(async () => {
      const { safeRedirect } = await import('/src/utils/session.js')
      return ['https://example.com', '//example.com', '/login', '/unknown', '/adoptions?status=1'].map(value => safeRedirect(value))
    })
    assert.deepEqual(redirects, ['/profile', '/profile', '/profile', '/profile', '/adoptions?status=1'])
    pass('Safe internal return destinations and lazy platform record loading')

    await page.getByRole('button', { name: '资料设置', exact: true }).click()
    await page.getByLabel('昵称', { exact: true }).fill('温暖同伴')
    await page.getByLabel('手机号', { exact: true }).fill('123')
    await page.getByRole('button', { name: '保存修改', exact: true }).click()
    await page.getByText('请输入正确的11位手机号', { exact: true }).waitFor()
    await page.getByLabel('手机号', { exact: true }).fill(session.phone)
    let profilePayload
    await page.route('**/api/auth/profile', async route => {
      profilePayload = route.request().postDataJSON()
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data: { ...session, ...profilePayload } }) })
    })
    await page.getByRole('button', { name: '保存修改', exact: true }).click()
    await page.locator('.saved-label').waitFor()
    assert.deepEqual(Object.keys(profilePayload).sort(), ['avatar', 'email', 'nickname', 'phone'])
    assert.match(await page.locator('.account-button').innerText(), /温暖同伴/)
    assert.match(await page.locator('.identity h2').innerText(), /温暖同伴/)
    await shot('profile-settings')
    await page.unroute('**/api/auth/profile')
    pass('Profile validation and intercepted save updates identity and header immediately')

    await page.getByRole('button', { name: '平台数据', exact: true }).click()
    await page.getByRole('heading', { name: '年龄与体重分布', exact: true }).waitFor()
    await page.locator('.platform-skeleton').waitFor({ state: 'hidden', timeout: 45000 })
    await settle()
    assert.equal(await page.locator('.chart canvas').count(), 4)
    assert.ok(recordRequests > 0)
    await page.locator('.type-filter').click()
    await page.getByRole('option', { name: '猫咪', exact: true }).click()
    await shot('profile-platform')
    await healthy()
    pass('Lazy platform charts: four charts render from live records and species filter works')

    await page.route('**/api/adoptions/my?**', route => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 503, message: 'UI检查：临时不可用' }) }))
    await visit('/profile')
    await page.locator('.recent-panel .empty-state').waitFor()
    assert.match(await page.locator('.personal-stats').innerText(), /—/)
    await page.unroute('**/api/adoptions/my?**')
    await page.getByRole('button', { name: '刷新资料', exact: true }).click()
    await page.locator('.recent-row').first().waitFor()
    await healthy()
    pass('Personal data failure displays retry state and recovers without losing the page')

    await page.getByRole('button', { name: '打开个人账号菜单' }).click()
    await page.getByRole('menuitem', { name: '退出登录' }).click()
    await page.waitForURL('**/login')
    assert.equal(await page.evaluate(() => localStorage.getItem('token')), null)
    await visit('/adoptions?status=1')
    assert.equal(new URL(page.url()).searchParams.get('redirect'), '/adoptions?status=1')
    await page.getByLabel('用户名', { exact: true }).fill(username)
    await page.getByLabel('登录密码', { exact: true }).fill(password)
    await page.getByLabel('登录密码', { exact: true }).press('Enter')
    await page.waitForURL('**/adoptions?status=1')
    await page.locator('.adoption-page').waitFor()
    await settle()
    assert.equal(await page.evaluate(() => JSON.parse(localStorage.getItem('user')).password), undefined)
    await shot('adoptions')
    await page.getByRole('button', { name: '待审核', exact: true }).click()
    await page.getByRole('heading', { name: '这个分类下还没有申请', exact: true }).waitFor()
    await page.getByRole('button', { name: '已通过', exact: true }).click()
    await page.locator('.application-card').first().waitFor()
    await page.locator('.application-details summary').first().click()
    assert.match(await page.locator('.application-details[open]').innerText(), /养宠经验/)
    pass('Application state filtering, correct empty state and expandable submitted details')
    pass('Real login with Enter, original application filter and logout clearing session')

    await visit('/profile')
    await noOverflow()
    await shot('profile')
    await visit('/adoptions')
    await noOverflow()
    await shot('adoptions')
    await page.setViewportSize({ width: 390, height: 844 })
    for (const route of ['/profile', '/adoptions']) { await visit(route); await noOverflow(); await shot(route.slice(1) + '-mobile') }
    await page.getByRole('button', { name: '展开导航', exact: true }).click()
    await page.locator('#primary-navigation').getByRole('link', { name: '寻找伙伴' }).click()
    await page.waitForURL('**/pets')
    await page.locator('#primary-navigation.open').waitFor({ state: 'hidden' })
    await settle()
    assert.equal(await page.locator('#primary-navigation.open').count(), 0)
    pass('390px personal pages and working mobile navigation')

    await page.getByRole('button', { name: '打开个人账号菜单' }).click()
    await page.getByRole('menuitem', { name: '退出登录' }).click()
    await page.waitForURL('**/login')
    for (const route of ['/login', '/register']) { await visit(route); await noOverflow(); await shot(route.slice(1) + '-mobile') }
    await healthy()
    pass('390px login and registration without overflow or runtime errors')
    console.log(JSON.stringify({ passed: checks.length, errors }, null, 2))
  } finally { await browser.close() }
}
run().catch(error => { console.error(error); process.exitCode = 1 })
