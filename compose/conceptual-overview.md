# Conceptual Overview of the Declarative UI Framework

## 1. High-Level Summary

The framework is a declarative, state-driven UI system. You define the UI's structure once during a "composition" phase. The UI is then "brought to life" by connecting it to explicit State objects.

The system is built on two core principles:

**Render Everything, Every Frame:** The framework does not use complex "dirty-tracking." Like Minecraft, it re-renders the entire UI every frame. This makes reactivity for properties (like text and color changes) simple, as elements just read the current value when they are drawn.

**Explicit Re-Composition:** The UI tree is "baked" after the initial composition for performance. However, subsets of this tree can be marked as dynamic. When an explicit State object changes, the framework will re-compose only the marked subset of the tree.

## 2. How the Tree Works

The UI's hierarchy (the "tree") is not stored inside the elements themselves.

There is a central, external UITree object that acts as the single source of truth. It stores all parent-child relationships.

Elements are "dumb"; they do not have an addChild method and do not hold their own list of children.

This design is key to enforcing the "immutable tree" philosophy. Because elements can't modify the tree, all structural changes must go through the framework's reactivity system.

For convenience, container elements have a getChildren() method, but it simply asks the central UITree for its list of children.

## 3. How the UI is Built (Composition)

You build the UI by implementing a compose method.

This method is run once to build the initial "baked" UI structure.

You are given an ICompositionContext object, which is the "key" that allows you to build the tree.

The context provides "scopes." An IContainerScope (for elements that can have children) has an e() method, which you use to add a child element. This method takes a lambda function (e.g., col -> { ... }) that defines the child's own scope.

An IElementScope (for elements that cannot have children) does not have this method, providing compile-time safety.

Once composition is finished, the context is gone, and the tree is "locked," preventing any further static changes except through the reactivity system.

## 4. The Core Interfaces

All components are based on a few core interfaces.

### Element Interfaces

**IElement:** The base interface for all UI elements. It defines the layout contract (measure, place) and rendering (draw).

**IPrimitiveElement (extends IElement):** "Terminator" elements that cannot have children. Their job is to draw pixels or handle simple interactions.

Examples: ElementLabel, ElementBox, ElementSlot, ElementImage

**IContainer (extends IElement):** Elements that can have children. The layout engine will recurse into them. This interface has two distinct implementation patterns:

#### Layout Containers (e.g., Stack, Column, Row)

These are "dumb" containers. Their only job is to arrange the children you give them according to a specific algorithm (e.g., stack vertically, arrange in a grid).

They have no internal state (beyond their children) and no complex behavior.

#### Composable Elements (e.g., Button, ScrollContainer)

These are "smart" components. They implement the IComposableElement interface.

They encapsulate behavior (like hover effects, click handling) and internal state (like isHovered).

They have an internal compose(ICompositionScope scope) method where they define their own default layout and children.

They expose a simple API (e.g., button.active.setText("Click")) for the 90% use case.

They expose SlotKey constants for advanced customization (see Section 7).

### Scope Interfaces (The "Keys")

Scopes are the objects passed to your configuration lambdas that let you build the UI.

**IElementScope:** Given when you configure an IPrimitiveElement (like ElementLabel). Provides active to access the element, but has no e() or bind() methods

**IContainerScope:** Given when you configure an IContainer (like Stack) OR when a composable element builds its internal UI.

- active: Access to the element (Stack, Column, etc.).
- e(IElement child, ...): Adds a new child element.
- bind(State<T>): Marks the current scope to be re-composed when the state changes.
- slot(SlotKey key, ...): (For component authors) Renders the content for a slot, executing either user-provided content or a default lambda.

**IComposableElementScope (extends IContainerScope):** Given to the user when they configure an IComposableElement (like Button).

- active: Access to the element (Button, etc.) to use its simple API (e.g., button.active.setText(...)).
- fillSlot(SlotKey key, ...): (For component users) Provides the content for one of the component's predefined slots.

## 5. How Layout Works

Layout is a classic two-phase system:

**Measure (Bottom-Up):** The framework asks the root element how much space it wants. The root element, in turn, asks its children, and so on, all the way down to the primitives. An ElementLabel measures its text, an ElementSlot reports its fixed 18x18 size, etc. This information flows back up the tree.

**Place (Top-Down):** Once the sizes are known, the framework gives the root element its final position and bounds. The root element (like a Column) then calculates and assigns the final bounds for each of its children, which in turn place their children. This flows back down the tree.

## 6. Where Data is Stored & How Reactivity Works

All application data is stored in State<T> objects. There are two distinct types of reactivity that use this state.

### Type 1: Reactive Properties (The "Provider" Model)

**What it's for:** Changing an element's simple properties every frame (e.g., a label's text, a box's color).

**How it works:** You do not use bind() for this. Instead, visual primitives (like ElementLabel) are designed to accept "Providers" for their properties. A provider can be a static value (a String), a Supplier<String> (a lambda), or a State<String>.

**Data Flow:** The element's draw() method is called every frame. Inside draw(), it simply calls .get() on its provider to get the most current value and draws it. This is simple, fast, and requires no explicit re-composition.

### Type 2: Reactive Structure (The "Bind" Model)

**What it's for:** Changing the structure of the UI (e.g., adding/removing items from a list, or swapping a component's internal appearance). This is for re-composing a subset of the tree.

**How it works:** During composition, inside a container's scope, you simply call scope.bind(myState).

**Data Flow:**

- The scope.bind(myState) call acts as a marker. It tells the framework: "Store the entire lambda function for this scope and associate it with myState."
- The framework then continues to execute the rest of the lambda once to build the initial static content for that scope.
- When you later call .set() on myState, the framework is notified. It finds all the lambdas bound to that state, clears all children from their respective containers, and re-executes the entire lambda to build a new set of children.

**Use Cases:**

- **Dynamic Lists:** Binding a Column to a State<List<String>> and using a for loop inside the lambda to create ElementLabel children.
- **Internal Component State:** A Button can bind an internal container to its own isHovered state. The lambda then uses an if statement to show either the "normal" or "hovered" appearance.

## 7. Customization with Slots

This model combines Type 2 reactivity (bind) with a "Slot" API to allow deep, safe customization of composable elements.

**The Problem:** How does a user change a Button's background without having to re-implement all its hover logic? The Button must own its isHovered state and its if statement, but the user must be able to provide the appearance.

**The Solution:** State-Specific Slots

**Component Author:** The Button author defines public constants:

```java
public static final SlotKey BACKGROUND_NORMAL = new SlotKey();
public static final SlotKey BACKGROUND_HOVERED = new SlotKey();
```

**Component Author:** Inside the Button's internal compose method, it uses bind() to create a dynamic region:

```java
// Inside Button.java
// ...
container.bind(this.isHovered); // Re-compose when hover changes

if (this.isHovered.get()) {
    container.slot(BACKGROUND_HOVERED, default -> { /* ... */ });
} else {
    container.slot(BACKGROUND_NORMAL, default -> { /* ... */ });
}
```

**Component User:** In their screen's compose method, they use the IComposableElementScope to provide the content for those slots:

```java
// Inside MyScreen.java
// ...
context.e(new Button(), button -> {
    button.fillSlot(Button.BACKGROUND_NORMAL, scope -> {
        scope.e(new ElementBox(Color.BLUE));
    });
    button.fillSlot(Button.BACKGROUND_HOVERED, scope -> {
        scope.e(new ElementBox(Color.RED));
    });
});
```

**How it's Wired:** The user's fillSlot calls populate an "ambient slot map" that is passed into the Button's internal compose method. When the Button's logic calls container.slot(), it looks in that map. If it finds the user's content for that key, it executes it; otherwise, it executes its own default lambda.