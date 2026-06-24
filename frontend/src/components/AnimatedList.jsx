import { motion, useInView, useReducedMotion } from 'motion/react';
import { useCallback, useEffect, useRef, useState } from 'react';
import './AnimatedList.css';

function AnimatedItem({ children, index, selected, onMouseEnter, onFocus }) {
  const ref = useRef(null);
  const reduceMotion = useReducedMotion();
  const inView = useInView(ref, { amount: 0.35, triggerOnce: false });

  return (
    <motion.div
      ref={ref}
      data-index={index}
      className={`animated-list-item ${selected ? 'selected' : ''}`}
      onMouseEnter={onMouseEnter}
      onFocus={onFocus}
      initial={reduceMotion ? false : { opacity: 0, y: 30, scale: 0.9 }}
      animate={reduceMotion || inView ? { opacity: 1, y: 0, scale: 1 } : { opacity: 0, y: 30, scale: 0.9 }}
      transition={reduceMotion ? { duration: 0 } : { type: 'spring', stiffness: 360, damping: 24, mass: 0.7 }}
    >
      {children}
    </motion.div>
  );
}

export default function AnimatedList({
  items = [],
  renderItem,
  getKey,
  showGradients = true,
  enableArrowNavigation = true,
  className = '',
  itemClassName = '',
  displayScrollbar = false,
  initialSelectedIndex = -1
}) {
  const listRef = useRef(null);
  const [selectedIndex, setSelectedIndex] = useState(initialSelectedIndex);
  const [keyboardNav, setKeyboardNav] = useState(false);
  const [topGradientOpacity, setTopGradientOpacity] = useState(0);
  const [bottomGradientOpacity, setBottomGradientOpacity] = useState(0);

  const handleScroll = useCallback((event) => {
    const { scrollTop, scrollHeight, clientHeight } = event.currentTarget;
    const bottomDistance = scrollHeight - (scrollTop + clientHeight);
    setTopGradientOpacity(Math.min(scrollTop / 44, 1));
    setBottomGradientOpacity(scrollHeight <= clientHeight ? 0 : Math.min(bottomDistance / 56, 1));
  }, []);

  useEffect(() => {
    const list = listRef.current;
    if (list) handleScroll({ currentTarget: list });
  }, [handleScroll, items]);

  useEffect(() => {
    if (!enableArrowNavigation) return undefined;

    function handleKeyDown(event) {
      const list = listRef.current;
      if (!list || !list.contains(document.activeElement)) return;

      if (event.key === 'ArrowDown') {
        event.preventDefault();
        setKeyboardNav(true);
        setSelectedIndex((prev) => Math.min((prev < 0 ? 0 : prev + 1), items.length - 1));
      }

      if (event.key === 'ArrowUp') {
        event.preventDefault();
        setKeyboardNav(true);
        setSelectedIndex((prev) => Math.max(prev - 1, 0));
      }
    }

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [enableArrowNavigation, items.length]);

  useEffect(() => {
    if (!keyboardNav || selectedIndex < 0 || !listRef.current) return;

    const selectedItem = listRef.current.querySelector(`[data-index="${selectedIndex}"]`);
    selectedItem?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    setKeyboardNav(false);
  }, [keyboardNav, selectedIndex]);

  return (
    <div className={`animated-list-container ${className}`}>
      <div
        ref={listRef}
        className={`animated-list ${displayScrollbar ? '' : 'no-scrollbar'}`}
        onScroll={handleScroll}
        tabIndex={0}
        role="list"
        aria-label="菜品列表"
      >
        {items.map((item, index) => (
          <AnimatedItem
            key={getKey ? getKey(item, index) : index}
            index={index}
            selected={selectedIndex === index}
            onMouseEnter={() => setSelectedIndex(index)}
            onFocus={() => setSelectedIndex(index)}
          >
            <div className={itemClassName} role="listitem">
              {renderItem ? renderItem(item, index) : item}
            </div>
          </AnimatedItem>
        ))}
      </div>
      {showGradients ? (
        <>
          <div className="animated-list-gradient top" style={{ opacity: topGradientOpacity }} />
          <div className="animated-list-gradient bottom" style={{ opacity: bottomGradientOpacity }} />
        </>
      ) : null}
    </div>
  );
}
