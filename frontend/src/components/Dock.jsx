import { LayoutGroup, motion, useMotionValue, useReducedMotion, useSpring, useTransform } from 'motion/react';
import { useRef } from 'react';
import './Dock.css';

function DockItem({ item, mouseX, spring, distance, magnification, baseItemSize }) {
  const ref = useRef(null);
  const reduceMotion = useReducedMotion();
  const labelSize = Math.max(baseItemSize, Math.min(112, String(item.label || '').length * 16 + 26));
  const mouseDistance = useTransform(mouseX, (value) => {
    const rect = ref.current?.getBoundingClientRect();
    if (!rect) return distance;
    return value - rect.left - rect.width / 2;
  });
  const targetScale = useTransform(
    mouseDistance,
    [-distance, 0, distance],
    [1, reduceMotion ? 1 : magnification, 1]
  );
  const scale = useSpring(targetScale, spring);

  function handleClick() {
    item.onClick();
    ref.current?.scrollIntoView({ behavior: reduceMotion ? 'auto' : 'smooth', inline: 'center', block: 'nearest' });
  }

  function handleFocus() {
    const rect = ref.current?.getBoundingClientRect();
    if (rect) mouseX.set(rect.left + rect.width / 2);
  }

  return (
    <motion.button
      ref={ref}
      type="button"
      style={{ width: labelSize, height: baseItemSize, scale }}
      onFocus={handleFocus}
      onBlur={() => mouseX.set(-100000)}
      onClick={handleClick}
      className={`dock-item ${item.active ? 'active' : ''} ${item.className || ''}`}
      aria-label={item.label}
      aria-pressed={item.active}
    >
      {item.active ? <motion.span className="dock-active-indicator" layoutId="category-dock-active" aria-hidden="true" /> : null}
      <span className="dock-icon" aria-hidden="true">{item.icon}</span>
    </motion.button>
  );
}

export default function Dock({
  items,
  className = '',
  spring = { mass: 0.12, stiffness: 180, damping: 18 },
  magnification = 1.16,
  distance = 110,
  panelHeight = 58,
  baseItemSize = 42
}) {
  const mouseX = useMotionValue(-100000);

  return (
    <div className="dock-outer">
      <LayoutGroup>
        <div
          className={`dock-panel ${className}`}
          style={{ minHeight: panelHeight }}
          onMouseMove={(event) => mouseX.set(event.clientX)}
          onMouseLeave={() => mouseX.set(-100000)}
          role="toolbar"
          aria-label="菜品分类"
        >
          {items.map((item) => (
            <DockItem
              key={item.id}
              item={item}
              mouseX={mouseX}
              spring={spring}
              distance={distance}
              magnification={magnification}
              baseItemSize={baseItemSize}
            />
          ))}
        </div>
      </LayoutGroup>
    </div>
  );
}
