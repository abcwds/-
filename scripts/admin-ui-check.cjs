const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert = require('node:assert/strict')
const path = require('node:path')

// Read-only UI checks: opens forms and cancels destructive confirmations, never saves data.
const base = process.env.ADMIN_UI_URL || 'http://127.0.0.1:3001'
const output = path.resolve(__dirname, '../test-results')
const errors = []
const checks = []
const pass = name => { checks.push(name); console.log(`PASS ${name}`) }

async function run() {
  const browser = await chromium.launch({ executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true })
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1080 } })
    // Block unexpected writes. Specific UI submission checks override this with mocks.
    await context.route('**/api/**', route => {
      const req = route.request()
      if (req.method() === 'GET' || (req.method() === 'POST' && req.url().endsWith('/api/auth/login'))) return route.continue()
      return route.fulfill({ status: 400, contentType: 'application/json', body: JSON.stringify({ code: 400, message: 'UI检查阻止了未模拟的写入' }) })
    })
    const page = await context.newPage()
    page.on('pageerror', error => errors.push(error.message))
    page.on('console', message => { if (message.type() === 'error' && /\[admin-ui\]|Failed to resolve|Uncaught/.test(message.text())) errors.push(message.text()) })
    const healthy = async () => {
      assert.equal(await page.locator('.fatal-error').count(), 0, 'No fatal error screen')
      assert.deepEqual(errors, [], 'No uncaught runtime errors')
    }
    const screenshot = async name => {
      await page.evaluate(() => { window.scrollTo(0, 0); document.querySelector('.workspace-content')?.scrollTo(0, 0) })
      await page.screenshot({ path: path.join(output, `admin-polished-${name}.png`), fullPage: true, animations: 'disabled', style: '.el-message { visibility: hidden !important; }' })
    }
    const settled = async () => {
      await page.waitForLoadState('networkidle')
      await page.locator('.el-loading-mask:visible').waitFor({ state: 'hidden', timeout: 15000 }).catch(async () => {
        assert.equal(await page.locator('.el-loading-mask:visible').count(), 0)
      })
      await healthy()
    }
    const visit = async route => {
      await page.goto(base + route)
      await settled()
      assert.equal(new URL(page.url()).pathname, route.split('?')[0])
    }
    const noOverflow = async () => {
      const sizes = await page.evaluate(() => ({ width: innerWidth, body: document.body.scrollWidth, content: document.querySelector('.workspace-content')?.clientWidth, contentScroll: document.querySelector('.workspace-content')?.scrollWidth }))
      assert.ok(sizes.body <= sizes.width + 1, JSON.stringify(sizes))
      if (sizes.content) assert.ok(sizes.contentScroll <= sizes.content + 1, JSON.stringify(sizes))
    }

    await visit('/login')
    await screenshot('login')
    await page.getByRole('button', { name: '登录管理工作台' }).click()
    await page.locator('.el-form-item__error').first().waitFor()
    assert.equal(await page.locator('.el-form-item__error').count(), 2)
    await healthy()
    await page.getByLabel('管理员账号', { exact: true }).fill(process.env.ADMIN_UI_USER || 'admin')
    await page.getByLabel('登录密码', { exact: true }).fill(process.env.ADMIN_UI_PASSWORD || '123456')
    await page.getByRole('button', { name: '登录管理工作台' }).click()
    await page.waitForURL('**/dashboard')
    await settled()
    assert.equal(await page.locator('.stat-card').count(), 4)
    assert.match(await page.locator('.stat-number').first().innerText(), /\d/)
    await screenshot('dashboard')
    pass('Login validation, authentication and live dashboard data')

    const routes = ['/users', '/pets', '/adoptions', '/health', '/vaccine', '/announcements']
    if (!process.argv.includes('--responsive')) {
    await page.getByRole('button', { name: '收起导航', exact: true }).click()
    await page.waitForTimeout(300)
    assert.ok((await page.locator('.sidebar').boundingBox()).width < 100)
    await page.getByRole('button', { name: '展开导航', exact: true }).click()
    await page.locator('.table-action').first().click()
    await page.getByRole('dialog', { name: '申请详情' }).waitFor()
    assert.ok((await page.locator('.detail-summary').innerText()).includes('领养申请'))
    await page.getByRole('dialog').getByRole('button', { name: '关闭此对话框' }).click()
    pass('Collapsed navigation and dashboard-to-application details')

    for (const route of routes) {
      await visit(route)
      assert.ok(await page.locator('.el-table__body-wrapper .el-table__row').count(), `Rows loaded on ${route}`)
      await noOverflow()
      await screenshot(route.slice(1))
      const search = page.locator('.search-field input')
      await search.fill('__no_matching_record_91827__')
      await search.press('Enter')
      await settled()
      assert.equal(await page.locator('.el-table__body-wrapper .el-table__row').count(), 0)
      assert.equal(await page.locator('.el-empty:visible').count(), 1)
      await search.fill('')
      await search.press('Enter')
      await settled()
      const nextPage = page.locator('.el-pager li.number').filter({ hasText: /^\s*2\s*$/ })
      if (await nextPage.count()) {
        const resource = route === '/users' ? 'users' : route.slice(1)
        const response = page.waitForResponse(res => {
          const url = new URL(res.url())
          return url.pathname === '/api/admin/' + resource && url.searchParams.get('page') === '2'
        })
        await nextPage.click()
        const result = await (await response).json()
        assert.equal(result.code, 200)
        assert.equal(result.data.current, 2)
        await settled()
        assert.equal(await page.locator('.el-table__body-wrapper .el-table__row').count(), result.data.records.length)
        await page.locator('.el-pager li.number').filter({ hasText: /^\s*1\s*$/ }).click()
        await settled()
      }
      if (route !== '/adoptions') {
        await page.getByRole('button', { name: '删除', exact: true }).first().click()
        await page.getByRole('button', { name: '取消', exact: true }).click()
        await healthy()
      }
      pass(`${route}: loaded records, search, pagination, one empty state and cancel confirmation`)
    }

    for (const [route, button] of [['/pets', '添加宠物'], ['/health', '添加记录'], ['/vaccine', '添加记录'], ['/announcements', '添加公告']]) {
      await visit(route)
      await page.getByRole('button', { name: button, exact: true }).click()
      const dialog = page.getByRole('dialog')
      await dialog.waitFor()
      await screenshot(`${route.slice(1)}-form`)
      await dialog.getByRole('button', { name: /^(保存档案|保存|保存并发布|保存草稿)$/ }).click()
      await dialog.locator('.el-form-item__error').first().waitFor()
      assert.ok(await dialog.locator('.el-form-item__error').count(), `Required fields on ${route}`)
      await healthy()
      await dialog.getByRole('button', { name: '取消', exact: true }).click()
      pass(`${route}: create form and required validation`)
    }
    }

    await visit('/adoptions?status=0')
    assert.match(await page.locator('.filter-select').innerText(), /待审核/)
    await page.locator('.filter-select').hover()
    await page.locator('.el-select__clear').click()
    await settled()
    assert.match(await page.locator('.table-heading').innerText(), /全部申请/)
    pass('Pending-application filter links')
    await page.setViewportSize({ width: 390, height: 844 })
    for (const route of ['/dashboard', ...routes]) {
      await visit(route)
      await noOverflow()
      await screenshot(`${route.slice(1)}-mobile`)
    }
    await page.getByRole('button', { name: '展开导航', exact: true }).click()
    await page.locator('.sidebar .nav-item').filter({ hasText: '宠物档案' }).click()
    await page.waitForURL('**/pets')
    await page.locator('.nav-backdrop').waitFor({ state: 'hidden' })
    await settled()
    assert.equal(await page.locator('.nav-backdrop').count(), 0)
    await page.getByRole('button', { name: '添加宠物', exact: true }).click()
    await page.getByRole('dialog').waitFor()
    await screenshot('pet-form-mobile')
    const bounds = await page.getByRole('dialog').boundingBox()
    assert.ok(bounds.x >= 0 && bounds.x + bounds.width <= 390)
    await page.getByRole('dialog').getByRole('button', { name: '取消', exact: true }).click()
    pass('390px: all seven pages, mobile navigation and form fit')

    await page.setViewportSize({ width: 1440, height: 1080 })
    await page.route('**/api/admin/pets?**', route => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 503, message: 'UI检查：临时不可用' }) }))
    await visit('/dashboard')
    assert.ok((await page.locator('.stat-number').first().innerText()).includes('—'))
    assert.ok(await page.locator('.load-alert').count())
    await page.unroute('**/api/admin/pets?**')
    await page.getByRole('button', { name: '刷新概览' }).click()
    await settled()
    assert.equal(await page.locator('.load-alert').count(), 0)
    await healthy()
    pass('Partial API failure and refresh recovery')

    await visit('/announcements')
    await page.getByRole('button', { name: '编辑', exact: true }).first().click()
    await page.getByRole('dialog').waitFor()
    await page.getByRole('dialog').getByRole('button', { name: '取消', exact: true }).click()
    await page.getByRole('dialog').waitFor({ state: 'hidden' })
    await page.getByRole('button', { name: '添加公告', exact: true }).click()
    await page.getByLabel('公告标题', { exact: true }).fill('界面检查，不会写入数据库')
    await page.getByLabel('正文内容', { exact: true }).fill('检查新建表单未携带上一条记录的字段。')
    let submitted
    await page.route('**/api/admin/announcements', async route => {
      assert.equal(route.request().method(), 'POST')
      submitted = route.request().postDataJSON()
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, message: '表单检查完成（未写入数据）' }) })
    })
    await page.getByRole('button', { name: '保存并发布', exact: true }).click()
    await page.getByRole('dialog').waitFor({ state: 'hidden' })
    assert.deepEqual(Object.keys(submitted).sort(), ['author', 'content', 'id', 'sortOrder', 'sourceName', 'sourceUrl', 'status', 'title', 'type'])
    assert.equal(submitted.id, null)
    await page.unroute('**/api/admin/announcements')
    pass('Edit-to-create form reset: intercepted submission contains no stale metadata')

    await page.setViewportSize({ width: 1440, height: 1320 })
    await visit('/dashboard')
    await screenshot('dashboard')
    await page.setViewportSize({ width: 1440, height: 1080 })
    await visit('/pets')
    await page.getByRole('button', { name: '添加宠物', exact: true }).click()
    await page.getByRole('dialog').waitFor()
    await screenshot('pets-form')
    await page.getByRole('dialog').getByRole('button', { name: '取消', exact: true }).click()
    await page.evaluate(() => { localStorage.removeItem('token'); localStorage.removeItem('adminUser') })
    await page.setViewportSize({ width: 390, height: 844 })
    await visit('/login')
    await noOverflow()
    await screenshot('login-mobile')
    pass('Final dashboard, form and mobile login visual capture')
    console.log(JSON.stringify({ passed: checks.length, runtimeErrors: errors }, null, 2))
  } finally { await browser.close() }
}
run().catch(error => { console.error(error); process.exitCode = 1 })
