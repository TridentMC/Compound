# Declarative UI Framework - Implementation Plan

This document outlines the sequential implementation plan for building the declarative UI framework.

---

## Phase 1: Foundation - Data Structures

**Goal**: Implement basic value types and layout data structures.

### 1.1 Layout Value Types
- [ ] `Size` class
  - Constructor, getters
  - Static constants (ZERO)
  - Utility methods (withWidth, withHeight)

- [ ] `Position` class
  - Constructor, getters
  - Static constants (ORIGIN)
  - Utility methods (offset)

- [ ] `Bounds` class
  - Constructors, getters
  - Hit testing (contains, intersects)
  - Utility methods (offset, shrink, expand)

### 1.2 Layout Configuration
- [ ] `Alignment` enum
  - All alignment constants
  - `align(Size childSize, Size parentSize)` calculation method

- [ ] `Constraints` class
  - Constructor, getters
  - Validation methods (hasFixedWidth, etc.)
  - Constraint operations (constrain, withMaxWidth, etc.)
  - Static factory methods (unbounded, fixed, loose, tight)
  - deflate() for padding

- [ ] `LayoutProperties` class
  - Builder pattern implementation
  - All property methods (withFixedSize, withPadding, etc.)
  - Getters for all properties

---

## Phase 2: Core Interfaces

**Goal**: Define all core interfaces as Java files.

### 2.1 Element Interfaces
- [ ] `IElement` interface
- [ ] `IPrimitiveElement` interface
- [ ] `IContainer` interface
- [ ] `IComposableElement` interface

### 2.2 Verify Compilation
- [ ] Ensure all interfaces compile
- [ ] Add proper imports and package declarations

---

## Phase 3: Tree Structure

**Goal**: Implement the tree node system and tree management.

### 3.1 TreeNode Implementation
- [ ] `ITreeNode` interface
- [ ] `TreeNode` class implementing ITreeNode
  - Element reference
  - Parent/children storage and management
  - State binding list
  - Composition function storage (Runnable)
  - Event handler storage (one field per handler type)
  - SlotMap reference
  - Tree query methods (getDepth, isAncestorOf, etc.)

### 3.2 UITree Implementation
- [ ] `UITree` class
  - Root node management
  - Element-to-node lookup (HashMap)
  - Node creation and attachment
  - Node detachment and cleanup
  - Tree traversal methods
  - Find operations (findNodesByElementType, findNodeAt)

### 3.3 Element Tree Integration
- [ ] Add tree injection mechanism
  - How elements get access to the tree
  - Probably via a field set during attachment
  - `IContainer.getChildren()` implementation pattern

---

## Phase 4: State & Reactivity

**Goal**: Implement observable state and the observer pattern.

### 4.1 State System
- [ ] `StateObserver` interface
- [ ] `State<T>` interface
- [ ] `StateImpl<T>` class
  - Value storage
  - Observer list
  - Notification on change
  - Lifecycle management (dispose)

### 4.2 TreeNode as StateObserver
- [ ] Make TreeNode implement StateObserver
- [ ] Implement `onStateChanged()` to trigger re-composition
- [ ] Wire up state binding registration
  - When `bind()` is called, register the node as observer

### 4.3 UITree Re-composition
- [ ] Implement `UITree.recomposeNode(ITreeNode node)`
  - Detach all children
  - Clear children list
  - Re-run the stored composition function
  - Re-attach to tree

### 4.4 State Change Handling
- [ ] Implement `UITree.onStateChanged(State<?> state)`
  - Find all nodes bound to this state
  - Trigger re-composition for each

---

## Phase 5: Slot System

**Goal**: Implement the slot customization mechanism.

### 5.1 Slot Data Structures
- [ ] `SlotKey` class
  - Name storage
  - equals/hashCode implementation

- [ ] `SlotContent` class
  - Lambda storage (Consumer<IContainerScope<?>>)
  - render() method to execute lambda

- [ ] `SlotMap` class
  - HashMap-based storage (SlotKey -> SlotContent)
  - put, get, has, clear methods

### 5.2 Slot Flow Design
- [ ] Design how SlotMap gets from `fillSlot()` to `slot()`
  - Option A: Pass through composition context
  - Option B: Store in TreeNode, access via scope
  - Document chosen approach

### 5.3 Slot Implementation
- [ ] Implement slot flow in scopes
- [ ] Test with simple composable element

---

## Phase 6: Composition System

**Goal**: Implement the scopes that allow building the UI.

### 6.1 Base Scope Implementation
- [ ] `IElementScope<T>` interface
- [ ] `ElementScope<T>` class
  - Holds reference to element (`active`)

- [ ] `IContainerScope<T>` interface
- [ ] `ContainerScope<T>` class
  - Holds reference to container (`active`)
  - Holds reference to current TreeNode
  - Holds reference to UITree
  - Implement `e()` method:
    - Create new element
    - Create TreeNode for element
    - Store configuration lambda in TreeNode
    - Execute configuration lambda with appropriate scope
    - Add TreeNode as child
    - Attach element to tree
  - Implement `bind()` method:
    - Register current TreeNode with state
    - Add state to node's bound states list

### 6.2 Composable Element Scope
- [ ] `IComposableElementScope<T>` interface
- [ ] `ComposableElementScope<T>` class extending ContainerScope
  - Holds SlotMap for user-provided content
  - Implement `fillSlot()`:
    - Store lambda in SlotMap
  - Pass SlotMap to element's compose() method somehow

### 6.3 Composition Scope
- [ ] `ICompositionScope` interface
- [ ] `CompositionScope` class extending ContainerScope
  - Holds reference to composable element's TreeNode
  - Holds SlotMap (received from parent scope)
  - Implement `useState()`:
    - Create State<T>
    - Tie to current element's lifecycle
  - Implement `slot()`:
    - Check SlotMap for user content
    - Execute user content if present
    - Execute default lambda if not

### 6.4 Composition Context
- [ ] `ICompositionContext` interface
- [ ] `CompositionContext` class extending ContainerScope
  - Holds reference to UITree
  - Holds list of screen-level states
  - Implement `useState()`:
    - Create State<T>
    - Add to screen-level state list
    - Return state

### 6.5 Scope Type Resolution
- [ ] Design system to determine which scope type to create
  - Check if element is IPrimitiveElement -> ElementScope
  - Check if element is IComposableElement -> ComposableElementScope
  - Check if element is IContainer -> ContainerScope
  - Create appropriate scope in `e()` method

---

## Phase 7: Event System

**Goal**: Implement event handling and dispatch.

### 7.1 Event Records
- [ ] `MouseClickEvent` record
- [ ] `MouseMoveEvent` record
- [ ] `MouseScrollEvent` record
- [ ] `KeyEvent` record
- [ ] Add consume/isConsumed state where needed

### 7.2 Event Handler Registration
- [ ] Add methods to elements for registering handlers
  - Elements call a helper method
  - Helper method gets the element's TreeNode from UITree
  - Stores handler in TreeNode

### 7.3 Event Dispatch in UITree
- [ ] `dispatchClick()`
  - Hit test to find node at position
  - Walk up tree firing handlers
  - Stop if consumed

- [ ] `dispatchMouseMove()`
  - Track which element is hovered
  - Fire exit/enter events as needed
  - Fire move events

- [ ] `dispatchScroll()`
  - Hit test to find node at position
  - Fire scroll handlers

- [ ] `dispatchKeyPress()` and `dispatchKeyRelease()`
  - Focused element concept?
  - Or broadcast to all?

### 7.4 Mouse Enter/Exit Tracking
- [ ] Track currently hovered element
- [ ] Fire exit when mouse leaves
- [ ] Fire enter when mouse enters
- [ ] Handle during mouse move events

---

## Phase 8: Layout Implementation

**Goal**: Implement the measure and place algorithms.

### 8.1 Base Element Layout
- [ ] Create `BaseElement` abstract class
  - Implements IElement
  - Stores: UITree reference, LayoutProperties, Bounds, visibility
  - Implements: lifecycle hooks (empty), getters/setters
  - Abstract: measure(), place(), draw()

### 8.2 Layout Container Base
- [ ] Create `BaseContainer` abstract class extending BaseElement
  - Implements IContainer
  - `getChildren()` queries UITree for node's children elements
  - Abstract: measure(), place() (each container has own algorithm)

### 8.3 Layout Algorithms
- [ ] `Stack` implementation
  - measure(): max of all children
  - place(): center or align all children in same space

- [ ] `Column` implementation
  - measure(): sum heights, max width
  - place(): stack children vertically with spacing
  - Handle weights for flexible children

- [ ] `Row` implementation
  - measure(): sum widths, max height
  - place(): arrange children horizontally with spacing
  - Handle weights for flexible children

- [ ] `Grid` implementation
  - measure(): calculate cell sizes
  - place(): position in grid cells

### 8.4 UITree Layout Coordination
- [ ] `measureTree(Constraints rootConstraints)`
  - Call measure on root element
  - Tree structure ensures bottom-up propagation

- [ ] `placeTree(Position rootPosition)`
  - Call place on root element with position and measured size
  - Tree structure ensures top-down propagation

---

## Phase 9: Primitive Elements

**Goal**: Implement basic rendering elements.

### 9.1 Base Primitive
- [ ] Create `BasePrimitiveElement` extending BaseElement
  - Implements IPrimitiveElement
  - Common functionality for primitives

### 9.2 Element Implementations
- [ ] `ElementSpacer`
  - measure(): return fixed or flexible size
  - place(): store bounds
  - draw(): do nothing

- [ ] `ElementBox`
  - Sprite storage (with Supplier support)
  - Color storage (with Supplier support)
  - measure(): respect layout properties
  - place(): store bounds
  - draw(): render 9-slice sprite with color

- [ ] `ElementLabel`
  - Text storage (String/Component with Supplier support)
  - Color storage (with Supplier support)
  - Shadow flag
  - measure(): measure text with font
  - place(): store bounds
  - draw(): render text

- [ ] `ElementImage`
  - Sprite storage (with Supplier support)
  - Size storage
  - Tint storage (with Supplier support)
  - measure(): return configured size
  - place(): store bounds
  - draw(): render sprite with tint

- [ ] `ElementSlot`
  - Slot reference
  - Highlighted flag (with Supplier support)
  - measure(): return 18x18
  - place(): store bounds
  - draw(): render slot background and item
  - Register click handler for slot interactions

---

## Phase 10: Composable Elements

**Goal**: Implement example smart components.

### 10.1 Base Composable
- [ ] Create `BaseComposableElement` extending BaseContainer
  - Implements IComposableElement
  - Stores slot keys
  - `getAvailableSlots()` returns defined slots
  - `useState()` helper that ties to lifecycle

### 10.2 Button Implementation
- [ ] `Button` class
  - Define slot keys (BACKGROUND_NORMAL, BACKGROUND_HOVERED, CONTENT)
  - Internal `isHovered` state
  - `setText()` API
  - `onClick()` API
  - Register mouse enter/exit handlers
  - `compose()` method:
    - Create Stack
    - Create dynamic background container
    - bind(isHovered)
    - Use if/else to show correct slot
    - Create content slot

### 10.3 ScrollContainer Implementation
- [ ] `ScrollContainer` class
  - Define slot key (CONTENT)
  - Internal scroll position state
  - Configuration APIs
  - Register scroll handler
  - `compose()` method:
    - Create clipping container
    - Offset content by scroll position
    - Optionally render scrollbar

---

## Phase 11: Screen Integration

**Goal**: Wire the framework into Minecraft's screen system.

### 11.1 Base Screen Class
- [ ] `CompoundScreenWithDeclarativeUI` abstract class
  - Holds UITree instance
  - Holds list of screen-level states
  - Abstract `compose(ICompositionContext)` method
  - `useState()` helper

### 11.2 Screen Lifecycle
- [ ] `init()` override
  - Create UITree
  - Create CompositionContext
  - Call compose() with context
  - Measure and place tree

- [ ] `render()` override
  - Re-measure and place if screen size changed
  - Walk tree and call draw() on all visible elements

- [ ] Event handling overrides
  - `mouseClicked()` -> UITree.dispatchClick()
  - `mouseMoved()` -> UITree.dispatchMouseMove()
  - `mouseScrolled()` -> UITree.dispatchScroll()
  - `keyPressed()` -> UITree.dispatchKeyPress()
  - `keyReleased()` -> UITree.dispatchKeyRelease()

- [ ] `removed()` override
  - Cleanup: dispose all states
  - Detach all elements
  - Clear tree

---

## Phase 12: Testing & Examples

**Goal**: Validate the framework with real examples.

### 12.1 Static Layout Test
- [ ] Create example from usage-example.md (chest UI)
- [ ] Verify layout is correct
- [ ] Verify rendering works

### 12.2 Dynamic List Test
- [ ] Create example from usage-example.md (enchanting screen)
- [ ] Verify bind() triggers re-composition
- [ ] Verify list updates correctly

### 12.3 Composable Element Test
- [ ] Create example from usage-example.md (button screen)
- [ ] Verify hover state works
- [ ] Verify slot customization works
- [ ] Verify simple API works

### 12.4 Interactive Test
- [ ] Create screen with buttons that modify state
- [ ] Verify events work
- [ ] Verify reactivity works
- [ ] Verify no memory leaks (states cleaned up)

---

## Implementation Notes

### Key Decisions to Make During Implementation

1. **Scope Creation**: How does `e()` determine which scope type to create?
   - Use instanceof checks on element type
   - Create appropriate scope subclass

2. **SlotMap Passing**: How does user's `fillSlot()` content reach element's `slot()`?
   - Store in TreeNode during composable element configuration
   - Pass through ICompositionScope when calling element.compose()

3. **Tree Injection**: How do elements get UITree reference?
   - Set during onAttached() call
   - Store as field in BaseElement

4. **Event Handler Registration**: How do elements register handlers?
   - Elements have helper method that looks up their own TreeNode
   - Stores handler in TreeNode

5. **Composition Function Storage**: What exactly gets stored in TreeNode?
   - The lambda passed to `e()` that configures the element
   - Gets re-run during re-composition

### Testing Strategy

- Unit test each phase before moving to next
- Create minimal examples to validate each feature
- Build progressively more complex examples
- Profile for performance issues
- Check for memory leaks (states, handlers cleaned up)

### Success Criteria

- All three examples from usage-example.md work correctly
- No memory leaks during re-composition
- Layout is pixel-perfect
- Events work reliably
- Re-composition is fast (< 16ms for typical UIs)
- Code is clean and maintainable
