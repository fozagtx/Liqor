// Slides the pinned Seeker's screens in step with the scroll: each screen holds while its step
// sits in the middle of the view, then glides to the next one at the same pace every time.
(() => {
  const steps = [...document.querySelectorAll('.step')]
  const stage = document.querySelector('.stage')
  const strip = document.querySelector('.stage .strip')
  const still = matchMedia('(prefers-reduced-motion: reduce)')
  let active = -1
  const update = () => {
    const vh = window.innerHeight
    const pinnedTop = stage.style.order !== undefined && getComputedStyle(stage).order === '-1'
    const top = pinnedTop ? stage.getBoundingClientRect().bottom : 0
    const ref = (top + vh) / 2
    const centers = steps.map((s) => {
      const r = s.querySelector('h2').getBoundingClientRect()
      return r.top + r.height / 2
    })
    let pos = 0
    if (ref >= centers[centers.length - 1]) pos = centers.length - 1
    else {
      for (let i = 0; i < centers.length - 1; i++) {
        if (ref >= centers[i] && ref < centers[i + 1]) {
          const f = (ref - centers[i]) / (centers[i + 1] - centers[i])
          const t = Math.min(1, Math.max(0, (f - 0.3) / 0.4))
          pos = i + t * t * (3 - 2 * t)
          break
        }
      }
    }
    if (still.matches) pos = Math.round(pos)
    strip.style.transform = `translate3d(${-pos * 100}%, 0, 0)`
    const now = Math.round(pos)
    if (now !== active) {
      active = now
      steps.forEach((s, i) => s.classList.toggle('is-active', i === now))
    }
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
