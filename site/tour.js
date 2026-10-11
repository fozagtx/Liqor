// The pinned Seeker plays every screen, one every 2 seconds. Scrolling to a step jumps to its first screen.
(() => {
  const steps = [...document.querySelectorAll('.step')]
  const stage = document.querySelector('.stage')
  const shots = [...document.querySelectorAll('.stage .screen img')]
  const still = matchMedia('(prefers-reduced-motion: reduce)')
  const narrow = matchMedia('(max-width: 820px)')
  const groups = steps.map((_, i) => shots.filter((img) => Number(img.dataset.step) === i))
  let active = -1
  let shown = 0
  let timer = 0

  const show = (img) => shots.forEach((s) => s.classList.toggle('is-on', s === img))

  const tick = () => {
    shown = (shown + 1) % shots.length
    show(shots[shown])
  }

  const update = () => {
    const top = narrow.matches ? Math.max(0, stage.getBoundingClientRect().bottom) : 0
    const ref = (top + window.innerHeight) / 2
    let now = 0
    steps.forEach((s, i) => {
      const r = s.querySelector('h2').getBoundingClientRect()
      if (r.top + r.height / 2 <= ref) now = i
    })
    if (now === active) return
    active = now
    shown = shots.indexOf(groups[now][0])
    steps.forEach((s, i) => s.classList.toggle('is-active', i === now))
    show(shots[shown])
    clearInterval(timer)
    if (!still.matches) timer = setInterval(tick, 2000)
  }

  let queued = false
  const schedule = () => {
    if (queued) return
    queued = true
    requestAnimationFrame(() => {
      queued = false
      update()
    })
  }

  window.addEventListener('scroll', schedule, { passive: true })
  window.addEventListener('resize', schedule)
  update()
})()
