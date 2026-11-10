const DOG_FALLBACKS = [
  { keyword: 'golden', image: '/uploads/pets/golden1.svg' },
  { keyword: 'labrador', image: '/uploads/pets/labrador1.svg' },
  { keyword: 'german', image: '/uploads/pets/german1.svg' },
  { keyword: 'shepherd', image: '/uploads/pets/german1.svg' },
  { keyword: 'husky', image: '/uploads/pets/husky1.svg' },
  { keyword: 'siberian', image: '/uploads/pets/husky1.svg' },
  { keyword: 'shiba', image: '/uploads/pets/shiba1.svg' },
  { keyword: 'poodle', image: '/uploads/pets/poodle1.svg' },
  { keyword: 'bulldog', image: '/uploads/pets/bulldog1.svg' }
]

const CAT_FALLBACKS = [
  { keyword: 'ragdoll', image: '/uploads/pets/ragdoll1.svg' },
  { keyword: 'persian', image: '/uploads/pets/persian1.svg' },
  { keyword: 'british', image: '/uploads/pets/british1.svg' },
  { keyword: 'scottish', image: '/uploads/pets/scottish1.svg' },
  { keyword: 'calico', image: '/uploads/pets/calico1.svg' },
  { keyword: 'orange', image: '/uploads/pets/orangetabby1.svg' },
  { keyword: 'tabby', image: '/uploads/pets/orangetabby1.svg' }
]

export const getPetFallbackImage = (pet) => {
  const type = String(pet?.type || '').toLowerCase()
  const breed = String(pet?.breed || '').toLowerCase()
  const list = type === 'cat' ? CAT_FALLBACKS : DOG_FALLBACKS
  return list.find(item => breed.includes(item.keyword))?.image
    || (type === 'cat' ? '/uploads/pets/ragdoll1.svg' : '/uploads/pets/golden1.svg')
}

export const resolvePetImage = (pet) => {
  const image = typeof pet === 'string' ? pet : pet?.image
  return image?.trim() || getPetFallbackImage(typeof pet === 'string' ? null : pet)
}

export const getPetImageStyle = (pet) => ({
  backgroundImage: `url("${getPetFallbackImage(typeof pet === 'string' ? null : pet)}")`,
  backgroundPosition: 'center',
  backgroundRepeat: 'no-repeat',
  backgroundSize: 'cover'
})

export const handlePetImageError = (event, pet) => {
  const fallback = getPetFallbackImage(pet)
  if (event.target.dataset.petFallback === fallback) return
  event.target.dataset.petFallback = fallback
  event.target.src = fallback
}
