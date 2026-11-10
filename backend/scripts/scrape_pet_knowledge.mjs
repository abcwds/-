import { writeFile, mkdir } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'

const outputPath = resolve('src/main/resources/pet-knowledge.json')

const AKC_CSV_URL = 'https://raw.githubusercontent.com/tmfilho/akcdata/master/data/akc-data-latest.csv'
const DOG_CEO_API = 'https://dog.ceo/api'
const CAT_API = 'https://api.thecatapi.com/v1'

const dogTargets = [
  { breed: 'Golden Retriever', dogCeoPath: 'retriever/golden' },
  { breed: 'Labrador Retriever', dogCeoPath: 'labrador' },
  { breed: 'German Shepherd Dog', dogCeoPath: 'german/shepherd' },
  { breed: 'Siberian Husky', dogCeoPath: 'husky' },
  { breed: 'Shiba Inu', dogCeoPath: 'shiba' },
  { breed: 'Beagle', dogCeoPath: 'beagle' },
  { breed: 'Boxer', dogCeoPath: 'boxer' },
  { breed: 'Samoyed', dogCeoPath: 'samoyed' }
]

const catTargets = [
  'Abyssinian',
  'American Shorthair',
  'British Shorthair',
  'Maine Coon',
  'Persian',
  'Ragdoll',
  'Siamese',
  'Sphynx'
]

function parseCsv(text) {
  const rows = []
  let row = []
  let value = ''
  let quoted = false

  for (let i = 0; i < text.length; i += 1) {
    const char = text[i]
    const next = text[i + 1]
    if (quoted) {
      if (char === '"' && next === '"') {
        value += '"'
        i += 1
      } else if (char === '"') {
        quoted = false
      } else {
        value += char
      }
      continue
    }
    if (char === '"') {
      quoted = true
    } else if (char === ',') {
      row.push(value)
      value = ''
    } else if (char === '\n') {
      row.push(value)
      rows.push(row)
      row = []
      value = ''
    } else if (char !== '\r') {
      value += char
    }
  }
  if (value || row.length) {
    row.push(value)
    rows.push(row)
  }

  const headers = rows.shift()
  return rows.map((cells) => Object.fromEntries(headers.map((header, index) => [header || 'breed', cells[index] || ''])))
}

async function getJson(url) {
  const response = await fetch(url, {
    headers: {
      'User-Agent': 'pet-adoption-graduation-project/1.0'
    }
  })
  if (!response.ok) {
    throw new Error(`${url} returned ${response.status}`)
  }
  return response.json()
}

function normalizeNumber(value) {
  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? parsed : null
}

function dogWeightKg(row) {
  const min = normalizeNumber(row.min_weight)
  const max = normalizeNumber(row.max_weight)
  if (!min || !max) return ''
  return `${Math.round(min)}-${Math.round(max)}kg`
}

function catWeightKg(weight) {
  return weight?.metric ? `${weight.metric.replace(/\s+/g, '')}kg` : ''
}

function makeDogAdvice(row) {
  const temperament = row.temperament || 'Friendly'
  const grooming = row.grooming_frequency_value ? `梳理频率建议约 ${row.grooming_frequency_value}/5。` : ''
  return `该犬种性格特征为 ${temperament}，适合有稳定运动和陪伴时间的家庭。${grooming}领养前建议评估居住空间、日常遛狗时间和疫苗接种计划。`
}

function makeCatAdvice(cat) {
  const grooming = cat.grooming ? `梳理需求 ${cat.grooming}/5。` : ''
  return `该猫种性格特征为 ${cat.temperament}，来源地区为 ${cat.origin}。${grooming}领养后建议准备安静适应期、猫砂盆和定期驱虫免疫计划。`
}

async function fetchDogs() {
  const csvResponse = await fetch(AKC_CSV_URL)
  if (!csvResponse.ok) throw new Error(`AKC CSV returned ${csvResponse.status}`)
  const rows = parseCsv(await csvResponse.text())
  const rowsByBreed = new Map(rows.map((row) => [row.breed, row]))

  const items = []
  for (const target of dogTargets) {
    const row = rowsByBreed.get(target.breed)
    if (!row) continue
    const image = await getJson(`${DOG_CEO_API}/breed/${target.dogCeoPath}/images/random`)
    items.push({
      type: 'Dog',
      breed: target.breed,
      nameZh: '',
      image: image.message,
      imageSource: 'Dog CEO',
      factSource: 'AKC Breed Info CSV',
      weightRange: dogWeightKg(row),
      lifeSpan: row.min_expectancy && row.max_expectancy ? `${row.min_expectancy}-${row.max_expectancy}年` : '',
      temperament: row.temperament,
      description: row.description,
      careAdvice: makeDogAdvice(row),
      sourceUrl: AKC_CSV_URL,
      imageSourceUrl: `${DOG_CEO_API}/breed/${target.dogCeoPath}/images/random`
    })
  }
  return items
}

async function fetchCats() {
  const breeds = await getJson(`${CAT_API}/breeds`)
  const items = []
  for (const target of catTargets) {
    const cat = breeds.find((item) => item.name === target)
    if (!cat) continue
    let image = ''
    if (cat.reference_image_id) {
      try {
        image = (await getJson(`${CAT_API}/images/${cat.reference_image_id}`)).url
      } catch {
        image = `https://cdn2.thecatapi.com/images/${cat.reference_image_id}.jpg`
      }
    }
    items.push({
      type: 'Cat',
      breed: cat.name,
      nameZh: '',
      image,
      imageSource: 'The Cat API',
      factSource: 'The Cat API breeds endpoint',
      weightRange: catWeightKg(cat.weight),
      lifeSpan: `${cat.life_span}年`,
      temperament: cat.temperament,
      description: cat.description,
      careAdvice: makeCatAdvice(cat),
      sourceUrl: `${CAT_API}/breeds`,
      imageSourceUrl: cat.reference_image_id ? `${CAT_API}/images/${cat.reference_image_id}` : `${CAT_API}/images/search`
    })
  }
  return items
}

const data = {
  generatedAt: new Date().toISOString(),
  sources: [
    {
      name: 'AKC Breed Info CSV',
      url: AKC_CSV_URL,
      usage: '犬种体重、寿命、性格、护理等级和简介'
    },
    {
      name: 'Dog CEO Dog API',
      url: `${DOG_CEO_API}/breeds/list/all`,
      usage: '犬只真实图片 URL'
    },
    {
      name: 'The Cat API',
      url: `${CAT_API}/breeds`,
      usage: '猫种体重、寿命、性格、来源、简介和图片 URL'
    }
  ],
  items: [...await fetchDogs(), ...await fetchCats()]
}

await mkdir(dirname(outputPath), { recursive: true })
await writeFile(outputPath, `${JSON.stringify(data, null, 2)}\n`, 'utf8')
console.log(`Wrote ${data.items.length} records to ${outputPath}`)
