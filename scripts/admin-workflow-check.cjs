const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert = require('node:assert/strict')
const path = require('node:path')
const base = process.env.ADMIN_UI_URL || 'http://127.0.0.1:3001'
const checks = []
const pass = name => { checks.push(name); console.log(`PASS ${name}`) }

async function run() {
  const browser = await chromium.launch({ executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true })
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    await context.route('**/api/**', route => {
      const req = route.request()
      if (req.method() === 'GET' || (req.method() === 'POST' && req.url().endsWith('/api/auth/login'))) return route.continue()
      return route.fulfill({ status: 400, contentType: 'application/json', body: JSON.stringify({ code: 400, message: 'UI检查阻止未模拟的写入' }) })
    })
    const auth = await context.request.post(base + '/api/auth/login', { data: { username: 'admin', password: '123456' } })
    const session = (await auth.json()).data
    assert.equal(session.role, 1)
    await context.addInitScript(value => {
      const { token, password, ...user } = value
      localStorage.setItem('token', token)
      localStorage.setItem('adminUser', JSON.stringify(user))
    }, session)
    const page = await context.newPage()
    const errors = []
    page.on('pageerror', error => errors.push(error.message))
    const settle = async () => { await page.waitForLoadState('networkidle'); assert.deepEqual(errors, []); assert.equal(await page.locator('.fatal-error').count(), 0) }
    const visit = async route => { await page.goto(base + route); await settle() }
    const dialog = name => page.getByRole('dialog', { name, exact: true })
    const shot = async name => {
      await page.screenshot({ path: path.resolve(__dirname, '../test-results/admin-refined-' + name + '.png'), animations: 'disabled', style: '.el-message { visibility: hidden !important; }' })
    }

    await visit('/users')
    await page.getByRole('button', { name: '详情', exact: true }).first().click()
    await dialog('用户详情').waitFor()
    for (const text of ['账号与联系方式', '手机号', '电子邮箱', '账号角色', '注册时间', '最近更新']) assert.ok((await dialog('用户详情').innerText()).includes(text), text)
    await shot('user-detail')
    await dialog('用户详情').getByRole('button', { name: '关闭详情' }).click()
    let userStatusPayload
    await page.route('**/api/admin/users/*/status', async route => {
      userStatusPayload = route.request().postDataJSON()
      await route.fulfill({ json: { code: 200, message: '模拟账号状态更新' } })
    })
    const accountSwitch = page.getByRole('switch').first()
    const previous = await accountSwitch.getAttribute('aria-checked')
    await page.locator('.el-switch').first().click()
    await page.waitForFunction(() => !document.querySelector('.el-switch.is-loading'))
    assert.equal(userStatusPayload.status, previous === 'true' ? 0 : 1)
    await page.waitForFunction(old => document.querySelector('.el-switch__input')?.getAttribute('aria-checked') !== old, previous)
    pass('User complete details and simulated account state update')

    await visit('/announcements')
    const classification = page.locator('.filter-panel .filter-select')
    const filtered = page.waitForResponse(response => response.url().includes('/api/admin/announcements?') && new URL(response.url()).searchParams.get('type') === 'news')
    await classification.click()
    await page.getByRole('option', { name: '养宠资讯', exact: true }).click()
    const classified = await (await filtered).json()
    assert.ok(classified.data.records.every(item => item.type === 'news'))
    await settle()
    await page.getByRole('button', { name: '预览', exact: true }).first().click()
    await dialog('公告预览').waitFor()
    assert.ok((await dialog('公告预览').innerText()).includes('来源与记录'))
    assert.ok((await dialog('公告预览').innerText()).includes('来源链接'))
    await shot('announcement-preview')
    await dialog('公告预览').getByRole('button', { name: '关闭预览' }).click()
    pass('Announcement classification reads real filtered data and source preview')

    await page.getByRole('button', { name: '编辑', exact: true }).first().click()
    await dialog('编辑公告').waitFor()
    for (const label of ['内容分类', '显示排序', '来源名称', '来源链接']) assert.ok((await dialog('编辑公告').innerText()).includes(label))
    await dialog('编辑公告').getByRole('button', { name: '取消', exact: true }).click()
    await dialog('编辑公告').waitFor({ state: 'hidden' })
    await page.getByRole('button', { name: '添加公告', exact: true }).click()
    await page.getByLabel('公告标题', { exact: true }).fill('界面交互检查')
    await page.getByLabel('正文内容', { exact: true }).fill('仅验证预览及请求内容，不写入数据库。')
    await page.getByLabel('作者', { exact: true }).fill('界面检查')
    await page.getByLabel('来源名称', { exact: true }).fill('检查来源')
    await page.getByLabel('来源链接', { exact: true }).fill('javascript:alert(1)')
    await page.getByRole('button', { name: '保存并发布', exact: true }).click()
    await page.getByText('请输入完整的 http(s) 来源链接', { exact: true }).waitFor()
    await page.getByLabel('来源链接', { exact: true }).fill('https://example.com/source')
    await page.getByRole('button', { name: '预览内容', exact: true }).click()
    await dialog('公告预览').waitFor()
    assert.ok((await dialog('公告预览').innerText()).includes('界面交互检查'))
    await dialog('公告预览').getByRole('button', { name: '关闭预览' }).click()
    let announcementPayload
    await page.route('**/api/admin/announcements', async route => {
      if (route.request().method() !== 'POST') return route.fallback()
      announcementPayload = route.request().postDataJSON()
      await route.fulfill({ json: { code: 200, message: '模拟保存成功' } })
    })
    await page.getByRole('button', { name: '保存并发布', exact: true }).click()
    await dialog('添加公告').waitFor({ state: 'hidden' })
    assert.equal(announcementPayload.id, null)
    assert.equal(announcementPayload.type, 'notice')
    assert.equal(announcementPayload.sortOrder, 0)
    assert.equal(announcementPayload.sourceUrl, 'https://example.com/source')
    let publicationPayload
    await page.route('**/api/admin/announcements/*/status', async route => {
      publicationPayload = route.request().postDataJSON()
      await route.fulfill({ json: { code: 200, message: '模拟发布状态更新' } })
    })
    await settle()
    const publication = page.getByRole('button', { name: /^(下架|发布)$/, exact: true }).first()
    const published = await publication.innerText()
    await publication.click()
    await page.getByRole('button', { name: published === '下架' ? '发布' : '下架', exact: true }).first().waitFor()
    assert.equal(publicationPayload.status, published === '下架' ? 0 : 1)
    pass('Announcement fields, source validation, unsaved preview, simulated save and publication')

    // The entire review resource is in memory; no real application is changed.
    const fixture = { id: 900001, userId: 2, petId: 349, petName: '审核交互检查', username: 'ui-check', realName: '界面检查', phone: '13900139000', address: '测试地址', workStatus: '稳定工作', housingType: '自有住房', hasOtherPet: 0, experience: '有照护经验', reason: '准备长期陪伴', status: 0, rejectReason: null, createdAt: '2026-11-01T10:00:00', updatedAt: '2026-11-01T10:00:00', adoptedAt: null }
    const reviewPayloads = []
    await page.route('**/api/admin/adoptions**', async route => {
      const url = new URL(route.request().url())
      if (route.request().method() === 'PUT') {
        const payload = route.request().postDataJSON()
        reviewPayloads.push(payload)
        Object.assign(fixture, payload, { updatedAt: '2026-11-03T12:00:00', adoptedAt: payload.status === 1 ? '2026-11-03T12:00:00' : null })
        return route.fulfill({ json: { code: 200, message: '模拟审核成功' } })
      }
      if (/\/adoptions\/\d+$/.test(url.pathname)) return route.fulfill({ json: { code: 200, data: { ...fixture } } })
      const status = url.searchParams.get('status')
      const records = status === null || status === '' || Number(status) === fixture.status ? [{ ...fixture }] : []
      return route.fulfill({ json: { code: 200, data: { records, total: records.length, current: 1, size: 10 } } })
    })
    await visit('/adoptions?status=0')
    await page.getByRole('button', { name: '详情', exact: true }).first().click()
    await dialog('申请详情').waitFor()
    for (const text of ['申请账号', '真实姓名', '工作状况', '住房类型', '养宠经验', '领养理由', '领养记录时间', '最近更新']) assert.ok((await dialog('申请详情').innerText()).includes(text), text)
    await shot('adoption-detail')
    await dialog('申请详情').getByRole('button', { name: '通过申请', exact: true }).click()
    await page.getByRole('button', { name: '再核对一下', exact: true }).click()
    assert.equal(reviewPayloads.length, 0)
    await dialog('申请详情').getByRole('button', { name: '通过申请', exact: true }).click()
    await page.getByRole('button', { name: '确认通过', exact: true }).click()
    await dialog('申请详情').getByText('已通过', { exact: true }).first().waitFor()
    assert.deepEqual(reviewPayloads, [{ status: 1 }])
    assert.ok((await dialog('申请详情').innerText()).includes('2026-11-03 12:00'))
    await dialog('申请详情').getByRole('button', { name: '关闭详情' }).click()
    fixture.status = 0
    await visit('/adoptions?status=0')
    await page.getByRole('button', { name: '拒绝', exact: true }).first().click()
    await dialog('拒绝申请').waitFor()
    await dialog('拒绝申请').getByRole('button', { name: '确认拒绝', exact: true }).click()
    await page.getByText('请填写拒绝理由', { exact: true }).waitFor()
    assert.equal(reviewPayloads.length, 1)
    await page.getByLabel('拒绝理由', { exact: true }).fill('  照护准备尚不充分，请完善后再申请。  ')
    await dialog('拒绝申请').getByRole('button', { name: '确认拒绝', exact: true }).click()
    await dialog('拒绝申请').waitFor({ state: 'hidden' })
    assert.deepEqual(reviewPayloads[1], { status: 2, rejectReason: '照护准备尚不充分，请完善后再申请。' })
    await visit('/adoptions?status=2')
    await page.getByRole('button', { name: '详情', exact: true }).first().click()
    await dialog('申请详情').getByText('照护准备尚不充分，请完善后再申请。', { exact: true }).waitFor()
    pass('Simulated review: complete details, approval confirmation, rejection validation and fresh result')
    await page.setViewportSize({ width: 390, height: 844 })
    const bounds = await dialog('申请详情').boundingBox()
    assert.ok(bounds.x >= 0 && bounds.x + bounds.width <= 390)
    assert.ok(await dialog('申请详情').evaluate(el => el.scrollWidth <= el.clientWidth + 1))
    await shot('adoption-detail-mobile')
    await settle()
    pass('Mobile review detail fits viewport; no uncaught errors')
    console.log(JSON.stringify({ passed: checks.length, errors, persistentBusinessWrites: 0 }, null, 2))
  } finally { await browser.close() }
}
run().catch(error => { console.error(error); process.exitCode = 1 })
