import { useEffect, useRef } from 'react'
import * as THREE from 'three'

/* Real-3D vault door wheel rendered with WebGL: three concentric brass
   ring assemblies + a spoked wheel, ambient spin, and a strong mouse-follow
   tilt (~30°) lerped every frame. Lazy-loaded so three.js only ships on
   the login route. */
export default function VaultDial3D() {
  const mountRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const mount = mountRef.current
    if (!mount) return
    const reducedMotion =
      typeof window.matchMedia === 'function' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches

    let renderer: THREE.WebGLRenderer
    try {
      renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
    } catch {
      return // no WebGL (jsdom, old GPU) — page works fine without the dial
    }
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.setSize(mount.clientWidth, mount.clientHeight)
    mount.appendChild(renderer.domElement)

    const scene = new THREE.Scene()
    const camera = new THREE.PerspectiveCamera(
      35,
      mount.clientWidth / mount.clientHeight,
      0.1,
      100,
    )
    camera.position.z = 11

    /* brass under a warm key light + green-tinted rim, matching the
       "Brass Vault" palette */
    scene.add(new THREE.AmbientLight(0x2a3530, 2.2))
    const key = new THREE.DirectionalLight(0xffe3b0, 2.6)
    key.position.set(-4, 5, 6)
    scene.add(key)
    const rim = new THREE.PointLight(0x5fae8c, 1.6, 30)
    rim.position.set(5, -3, 4)
    scene.add(rim)

    const brass = new THREE.MeshStandardMaterial({ color: 0xc9a35c, metalness: 0.9, roughness: 0.32 })
    const brassDark = new THREE.MeshStandardMaterial({ color: 0x6e5a35, metalness: 0.85, roughness: 0.45 })

    const group = new THREE.Group()
    const outer = new THREE.Group()
    const mid = new THREE.Group()
    const wheel = new THREE.Group()

    // outer ring + tick marks
    outer.add(new THREE.Mesh(new THREE.TorusGeometry(3.6, 0.06, 16, 96), brassDark))
    const tick = new THREE.BoxGeometry(0.05, 0.28, 0.05)
    for (let i = 0; i < 36; i++) {
      const a = (i / 36) * Math.PI * 2
      const m = new THREE.Mesh(tick, i % 3 === 0 ? brass : brassDark)
      m.position.set(Math.cos(a) * 3.25, Math.sin(a) * 3.25, 0)
      m.rotation.z = a + Math.PI / 2
      outer.add(m)
    }

    // mid ring, slightly forward
    const midRing = new THREE.Mesh(new THREE.TorusGeometry(2.5, 0.1, 16, 96), brass)
    mid.add(midRing)
    const stud = new THREE.SphereGeometry(0.09, 12, 12)
    for (let i = 0; i < 8; i++) {
      const a = (i / 8) * Math.PI * 2
      const m = new THREE.Mesh(stud, brassDark)
      m.position.set(Math.cos(a) * 2.5, Math.sin(a) * 2.5, 0.08)
      mid.add(m)
    }
    mid.position.z = 0.45

    // vault wheel: rim + spokes + hub, clearly in front
    wheel.add(new THREE.Mesh(new THREE.TorusGeometry(1.45, 0.13, 20, 72), brass))
    const spoke = new THREE.CylinderGeometry(0.07, 0.07, 2.9, 16)
    for (let i = 0; i < 3; i++) {
      const m = new THREE.Mesh(spoke, brass)
      m.rotation.z = (i / 3) * Math.PI
      wheel.add(m)
    }
    const hub = new THREE.Mesh(new THREE.CylinderGeometry(0.38, 0.38, 0.5, 32), brassDark)
    hub.rotation.x = Math.PI / 2
    wheel.add(hub)
    wheel.position.z = 0.9

    group.add(outer, mid, wheel)
    scene.add(group)

    // mouse tracking: normalized -1..1 over the viewport, strong tilt
    let targetX = 0
    let targetY = 0
    const onMove = (e: MouseEvent) => {
      targetX = (e.clientX / window.innerWidth) * 2 - 1
      targetY = (e.clientY / window.innerHeight) * 2 - 1
    }
    if (!reducedMotion) window.addEventListener('mousemove', onMove)

    /* canvas spans the whole scene; scale the wheel to ~a 780px-box look and
       park it on the layout's grid split line (1.1fr / 1fr) */
    const WHEEL_DIAMETER_WORLD = 7.4
    const visibleH = 2 * camera.position.z * Math.tan((camera.fov * Math.PI) / 360)
    let baseX = 0
    const onResize = () => {
      const w = mount.clientWidth
      const h = mount.clientHeight
      if (!w || !h) return
      camera.aspect = w / h
      camera.updateProjectionMatrix()
      renderer.setSize(w, h)
      const pxPerWorld = h / visibleH
      const targetPx = Math.min(780, 0.92 * Math.min(w, h))
      group.scale.setScalar(targetPx / (WHEEL_DIAMETER_WORLD * pxPerWorld))
      const splitFrac = w > 900 ? 1.1 / 2.1 : 0.5
      baseX = ((splitFrac - 0.5) * w) / pxPerWorld
      group.position.x = baseX + targetX * 0.5
    }
    onResize()
    const ro = new ResizeObserver(onResize)
    ro.observe(mount)

    let raf = 0
    const animate = () => {
      raf = requestAnimationFrame(animate)
      if (!reducedMotion) {
        outer.rotation.z += 0.0008
        mid.rotation.z -= 0.0014
        wheel.rotation.z += 0.0026
        // ~30° max tilt toward the cursor + slight positional parallax
        group.rotation.y += (targetX * 0.55 - group.rotation.y) * 0.06
        group.rotation.x += (targetY * 0.45 - group.rotation.x) * 0.06
        group.position.x += (baseX + targetX * 0.5 - group.position.x) * 0.06
        group.position.y += (-targetY * 0.3 - group.position.y) * 0.06
      }
      renderer.render(scene, camera)
    }
    animate()

    return () => {
      cancelAnimationFrame(raf)
      ro.disconnect()
      window.removeEventListener('mousemove', onMove)
      scene.traverse((obj) => {
        if (obj instanceof THREE.Mesh) obj.geometry.dispose()
      })
      brass.dispose()
      brassDark.dispose()
      renderer.dispose()
      mount.removeChild(renderer.domElement)
    }
  }, [])

  return <div ref={mountRef} className="dial-webgl" />
}
