const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert = require('node:assert/strict')
const path = require('node:path')
const fs = require('node:fs')

const base = process.env.ADMIN_UI_URL || 'http://127.0.0.1:3001'
const output = path.resolve(__dirname, '../test-results')
const errors = []
const blockedWrites = []
const checks = []
const issues = []
const pass = name => { checks.push(name); console.log('PASS ' + name) }
const escape = value => String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

async function run() {
  fs.mkdirSync(output, { recursive: true })
  const browser = await chromium.launch({ executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true })
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1080 }, serviceWorkers: 'block' })
    // This context cannot write data: the sole allowed POST is authentication.
    await context.route('**/*', async route => {
      const req = route.request()
      const url = new URL(req.url())
      if (req.method() === 'GET' || (req.method() === 'POST' && url.origin === new URL(base).origin && url.pathname === '/api/auth/login')) return route.continue()
      blockedWrites.push(req.method() + ' ' + url.pathname)
      return route.fulfill({ status: 403, contentType: 'application/json', body: JSON.stringify({ code: 403, message: '只读检查已阻止写入' }) })
    })
    const page = await context.newPage()
    const pendingApi = new Set()
    page.on('request', request => { if (new URL(request.url()).pathname.startsWith('/api/')) pendingApi.add(request) })
    page.on('requestfinished', request => pendingApi.delete(request))
    page.on('requestfailed', request => pendingApi.delete(request))
    page.on('pageerror', error => errors.push(error.message))
    const healthy = async () => {
      assert.equal(await page.locator('.fatal-error').count(), 0, 'No fatal error screen')
      assert.deepEqual(errors, [], 'No runtime exceptions')
    }
    const settled = async () => {
      await page.waitForLoadState('domcontentloaded')
      await page.waitForTimeout(150)
      const deadline = Date.now() + 15000
      while (pendingApi.size && Date.now() < deadline) await page.waitForTimeout(50)
      assert.equal(pendingApi.size, 0, 'API requests completed')
      await page.locator('.el-loading-mask:visible').first().waitFor({ state: 'hidden', timeout: 15000 })
      assert.equal(await page.locator('.el-loading-mask:visible').count(), 0, 'Loading completed')
      await healthy()
    }
    const visit = async route => { await page.goto(base + route); await settled() }
    const apiGet = async endpoint => page.evaluate(async endpoint => {
      const res = await fetch('/api' + endpoint, { headers: { Authorization: 'Bearer ' + localStorage.getItem('token') } })
      const body = await res.json()
      if (res.status !== 200 || body.code !== 200) throw new Error('Read-only data request failed: ' + endpoint)
      return body.data
    }, endpoint)
    const waitListRequest = (resource, predicate) => page.waitForResponse(response => {
      const url = new URL(response.url())
      return response.request().method() === 'GET' && url.pathname === '/api/admin/' + resource && predicate(url.searchParams)
    })
    const readResponse = async promise => { const res = await promise; assert.equal(res.status(), 200); const body = await res.json(); assert.equal(body.code, 200); await settled(); return body.data }
    const closeEdit = async dialog => { await dialog.getByRole('button', { name: '取消', exact: true }).click(); await dialog.waitFor({ state: 'hidden' }); await healthy() }
    const screenshot = async (kind, dialog) => {
      await dialog.locator('.el-dialog__body').evaluate(element => { element.scrollTop = 0 })
      await page.screenshot({ path: path.join(output, 'admin-refined-' + kind + '-detail.png'), fullPage: true, animations: 'disabled', style: '.el-message { visibility: hidden !important; }' })
    }
    const field = (dialog, label) => dialog.locator('.el-form-item').filter({ has: page.locator('.el-form-item__label').filter({ hasText: new RegExp('^' + escape(label) + '$') }) })
    const selectPet = async (select, pet) => {
      await select.click()
      const input = select.locator('input').first()
      await input.fill(pet.name)
      const option = page.getByRole('option').filter({ has: page.locator('.pet-option strong', { hasText: new RegExp('^' + escape(pet.name) + '$') }) }).filter({ hasText: '#' + pet.id + ' ·' })
      await option.waitFor({ state: 'visible' })
      await option.click()
      await settled()
    }

    await visit('/login')
    await page.getByLabel('管理员账号', { exact: true }).fill(process.env.ADMIN_UI_USER || 'admin')
    await page.getByLabel('登录密码', { exact: true }).fill(process.env.ADMIN_UI_PASSWORD || '123456')
    await page.getByRole('button', { name: '登录管理工作台' }).click()
    await page.waitForURL('**/dashboard')
    await settled()
    pass('Authenticated admin with context-wide write blocking')

    const pets = await apiGet('/admin/pets?page=1&size=30')
    assert.ok(pets.records.length, 'Live pets exist')
    const chosenPet = pets.records[0]
    const configs = [
      { route: '/pets', resource: 'pets', title: '宠物完整档案', edit: '编辑宠物', editButton: '编辑档案', labels: ['档案编号', '名称', '类型', '品种', '年龄', '性别', '体重', '毛色', '描述', '领养状态', '健康状况', '领养费用', '救助站', '资料来源', '来源核验日期', '资料链接', '图片来源', '图片原始链接', '当前图片地址', '核验说明', '创建时间', '最后更新时间'] },
      { route: '/health', resource: 'health', title: '健康记录详情', edit: '编辑健康记录', editButton: '编辑记录', labels: ['记录编号', '宠物编号', '宠物名称', '记录日期', '体重', '体温', '诊断结果', '症状', '处理措施', '医生', '医院', '费用', '录入时间', '备注', '来源名称', '来源链接', '核验说明'] },
      { route: '/vaccine', resource: 'vaccine', title: '疫苗记录详情', edit: '编辑疫苗记录', editButton: '编辑记录', labels: ['记录编号', '宠物编号', '宠物名称', '录入时间', '疫苗名称', '疫苗类型', '剂量', '接种日期', '下次接种计划', '计划说明', '医院', '医生', '备注', '来源名称', '来源链接', '核验说明'] }
    ]

    for (const config of configs) {
      await visit(config.route)
      const data = await apiGet('/admin/' + config.resource + '?page=1&size=10')
      assert.ok(data.records.length, 'Live records exist for ' + config.resource)
      const first = data.records[0]
      const row = page.locator('.el-table__body-wrapper .el-table__row').first()
      await row.getByRole('button', { name: '详情', exact: true }).click()
      const detail = page.getByRole('dialog', { name: config.title, exact: true })
      await detail.waitFor()
      await detail.locator('.record-detail-sections').waitFor()
      await healthy()
      const actualLabels = await detail.locator('.detail-field dt').allTextContents()
      for (const label of config.labels) assert.ok(actualLabels.includes(label), config.resource + ': detail field ' + label)
      const detailRead = await apiGet('/admin/' + config.resource + '/' + first.id)
      const idLabel = config.resource === 'pets' ? '档案编号' : '记录编号'
      const idValue = detail.locator('.detail-field').filter({ has: page.locator('dt').filter({ hasText: new RegExp('^' + idLabel + '$') }) }).locator('dd')
      assert.equal((await idValue.innerText()).trim(), String(detailRead.id), 'Detail matches live record ID')
      for (const [key, label] of config.resource === 'pets' ? [['name', '名称'], ['description', '描述'], ['sourceNote', '核验说明']] : config.resource === 'health' ? [['diagnosis', '诊断结果'], ['treatment', '处理措施'], ['notes', '备注']] : [['vaccineName', '疫苗名称'], ['dose', '剂量'], ['notes', '备注']]) {
        if (detailRead[key]) {
          const value = detail.locator('.detail-field').filter({ has: page.locator('dt').filter({ hasText: new RegExp('^' + label + '$') }) }).locator('dd')
          assert.equal((await value.innerText()).trim(), String(detailRead[key]).trim(), config.resource + ': live value ' + key)
        }
      }
      await screenshot(config.resource, detail)
      await detail.getByRole('button', { name: config.editButton, exact: true }).click()
      const edit = page.getByRole('dialog', { name: config.edit, exact: true })
      await edit.waitFor()
      await settled()
      if (config.resource === 'pets') assert.equal(await field(edit, '名称').locator('input').inputValue(), detailRead.name)
      else assert.equal(Number(await field(edit, '宠物 ID').locator('input').inputValue()), detailRead.petId)
      await closeEdit(edit)
      pass(config.resource + ': all detail fields, live values, screenshot and detail-to-edit')
    }

    await visit('/pets')
    const typeSelect = page.locator('.search-bar .filter-select').first()
    for (const [label, code] of [['犬', 'Dog'], ['猫', 'Cat']]) {
      await typeSelect.click()
      const response = waitListRequest('pets', params => params.get('type') === code && params.get('page') === '1')
      await page.getByRole('option', { name: label, exact: true }).click()
      const result = await readResponse(response)
      assert.ok(result.records.every(record => record.type === code), 'Backend returns selected type only')
    }
    await typeSelect.hover()
    let response = waitListRequest('pets', params => !params.get('type') && params.get('page') === '1')
    await typeSelect.locator('.el-select__clear').click()
    const allPets = await readResponse(response)
    assert.equal(allPets.total, pets.total)
    pass('Pet type Dog/Cat sent to backend; clearing restores all records on page 1')

    for (const config of configs.filter(config => config.resource !== 'pets')) {
      await visit(config.route)
      const baseline = await apiGet('/admin/' + config.resource + '?page=1&size=10')
      const filter = page.locator('.search-bar .record-pet-select')
      response = waitListRequest(config.resource, params => params.get('petId') === String(chosenPet.id) && params.get('page') === '1')
      await selectPet(filter, chosenPet)
      const filtered = await readResponse(response)
      assert.ok(filtered.records.every(record => record.petId === chosenPet.id))
      await filter.hover()
      response = waitListRequest(config.resource, params => !params.get('petId') && params.get('page') === '1')
      await filter.locator('.el-select__clear').click()
      const cleared = await readResponse(response)
      assert.equal(cleared.total, baseline.total)

      await page.getByRole('button', { name: '添加记录', exact: true }).click()
      const create = page.getByRole('dialog', { name: config.resource === 'health' ? '添加健康记录' : '添加疫苗记录', exact: true })
      await create.waitFor()
      await selectPet(create.locator('.record-pet-select'), chosenPet)
      assert.equal(Number(await field(create, '宠物 ID').locator('input').inputValue()), chosenPet.id, 'Selected pet ID is synchronized')
      assert.equal(await field(create, '宠物名称').locator('input').inputValue(), chosenPet.name, 'Selected pet name is synchronized')
      const otherPet = pets.records.find(pet => pet.id !== chosenPet.id)
      if (otherPet) {
        const idInput = field(create, '宠物 ID').locator('input')
        await idInput.fill(String(otherPet.id))
        await idInput.press('Tab')
        await settled()
        const actualName = await field(create, '宠物名称').locator('input').inputValue()
        if (actualName !== otherPet.name) { issues.push(config.resource + ': manual pet ID did not retain resolved pet name'); console.log('FAIL ' + issues.at(-1)) }
      }
      await closeEdit(create)
      pass(config.resource + ': petId filtering, clear recovery, selector ID/name sync and manual ID compatibility')

      await page.getByRole('button', { name: '添加记录', exact: true }).click()
      await create.waitFor()
      await create.getByRole('button', { name: '保存', exact: true }).click()
      await create.locator('.el-form-item__error').first().waitFor()
      assert.ok(await create.locator('.el-form-item__error').count(), 'Empty form shows required validation')
      await closeEdit(create)
      await page.locator('.el-table__body-wrapper .el-table__row').first().getByRole('button', { name: '编辑', exact: true }).click()
      const edit = page.getByRole('dialog', { name: config.edit, exact: true })
      await edit.waitFor()
      await settled()
      const remainingErrors = await edit.locator('.el-form-item__error').count()
      if (remainingErrors) { issues.push(config.resource + ': create validation errors remained in existing-record edit form'); console.log('FAIL ' + issues.at(-1)) }
      else pass(config.resource + ': empty-create validation does not remain after opening edit')
      await closeEdit(edit)
    }
    await healthy()
    assert.deepEqual(blockedWrites, [], 'No writes were attempted by these read-only actions')
    console.log(JSON.stringify({ passed: checks.length, issues, runtimeErrors: errors.length, blockedWrites: blockedWrites.length, screenshots: configs.map(config => 'test-results/admin-refined-' + config.resource + '-detail.png') }))
    if (issues.length) process.exitCode = 1
  } finally {
    await browser.close()
  }
}

run().catch(error => { console.error(error.message); process.exitCode = 1 })
