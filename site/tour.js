// Lights up the step nearest the middle of the screen and shows its screen in the pinned Seeker.
(() => {
  const steps = [...document.querySelectorAll('.step')]
  const shots = [...document.querySelectorAll('.stage .screen img')]
  let active = 0
  const update = () => {
    const mid = window.innerHeight / 2
    let best = 0
    let bestDist = Infinity
    steps.forEach((step, i) => {
      const r = step.querySelector('h2').getBoundingClientRect()
      const d = Math.abs(r.top + r.height / 2 - mid)
      if (d < bestDist) {
        bestDist = d
        best = i
      }
    })
    if (best === active) return
    active = best
    steps.forEach((s, i) => s.classList.toggle('is-active', i === best))
    shots.forEach((s, i) => s.classList.toggle('is-on', i === best))
  }
  let queued = false
  window.addEventListener(
    'scroll',
    () => {
      if (queued) return
      queued = true
      requestAnimationFrame(() => {
        queued = false
        update()
      })
    },
    { passive: true },
  )
  window.addEventListener('resize', update)
  update()
})()
