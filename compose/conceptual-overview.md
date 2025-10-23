# **Conceptual Overview of the Declarative UI Framework**

## **1. High-Level Summary**

The framework is a **declarative, state-driven UI system**. You define the UI's structure once during a "composition" phase. The UI is then "brought to life" by connecting it to explicit **State** objects.  
The system is built on two core principles:

1. **Render Everything, Every Frame:** The framework does not use complex "dirty-tracking." Like Minecraft, it re-renders the entire UI every frame. This makes reactivity for properties (like text and color changes) simple, as elements just read the current value when they are drawn.  
2. **Explicit Re-Composition:** The UI tree is "baked" after the initial composition for performance. However, subsets of this tree can be marked as dynamic. When a an explicit **State** object changes, the framework will re-compose *only the marked subset* of the tree.

## **2. How the Tree Works**

The UI's hierarchy (the "tree") is **not stored inside the elements themselves.**

* There is a central, external **UITree** object that acts as the single source of truth. It stores all parent-child relationships.  
* Elements are "dumb"; they do not have an addChild method and do not hold their own list of children.  
* This design is key to enforcing the "immutable tree" philosophy. Because elements can't modify the tree, all structural changes *must* go through the framework's reactivity system.  
* For convenience, container elements have a getChildren() method, but it simply asks the central UITree for its list of children.

## **3. How the UI is Built (Composition)**

You build the UI by implementing a composeUI method.

* This method is run once to build the initial "baked" UI structure.  
* You are given a **CompositionContext** object, which is the "key" that allows you to build the tree.  
* The context provides "scopes." A **ContainerScope** (for elements that can have children) has an **e()** method, which you use to add a child element. This method takes a lambda function (e.g., col -> { ... }) that defines the child's *own* scope.  
* A "terminator" scope (for elements that *cannot* have children) does not have this method, providing compile-time safety.  
* Once composition is finished, the CompositionContext is gone, and the tree is "locked," preventing any further static changes *except* through the reactivity system.

## **4. The Components (Elements)**

All components are based on a base IElement interface. The primary distinction is between "Terminator" and "Container" elements.

1. **Terminator Elements (Implement IElement)**  
   * These are elements that **cannot** have children.  
   * Their job is to draw pixels or handle specific, simple interactions.  
   * During composition, they are given an ElementScope, which does *not* have an e() or bind() method.  
   * **Examples:** ElementLabel (draws text), ElementSlot (draws an item stack), ElementImage (draws a texture), ElementSpacer.  
2. **Container Elements (Implement IContainer)**  
   * These are elements that **can** have children.  
   * During composition, they are given a ContainerScope, which *does* have the e() and bind() methods.  
   * This category has two sub-types:  
     * **Layout-Only Containers:** Their primary job is to **arrange children**. They typically don't draw anything themselves.  
       * **Examples:** Stack, Column, Row, Grid.  
     * **Visual Containers:** They **both draw something** (like a panel) **and arrange children**.  
       * **Examples:** ElementBox, ElementButton, ScrollContainer.

## **5. How Layout Works**

Layout is a classic **two-phase system**:

1. **Measure (Bottom-Up):** The framework asks the root element how much space it wants. The root element, in turn, asks its children, and so on, all the way down to the terminators. An ElementLabel measures its text, an ElementSlot reports its fixed 18x18 size, etc. This information flows back *up* the tree.  
2. **Place (Top-Down):** Once the sizes are known, the framework gives the root element its final position and bounds. The root element (like a Column) then calculates and assigns the final bounds for each of its children, which in turn place *their* children. This flows back *down* the tree.

## **6. Where Data is Stored & How Reactivity Works**

All application data (like a list of items, a player's name, or a selected enchantment) is stored in **State<T>** objects. There are two distinct types of reactivity that use this state.

### **Type 1: Reactive Properties (The "Provider" Model)**

* **What it's for:** Changing an element's properties every frame (e.g., a label's text, a box's color, a button's hover state).  
* **How it works:** You do **not** use bind() for this. Instead, visual primitives (like ElementLabel) are designed to accept "Providers" for their properties. A provider can be a static value (a String), a Supplier (a lambda function), or a State<T> object.  
* **Data Flow:** The element's draw() method is called every frame. Inside draw(), it simply calls .get() on its provider to get the *most current value* and draws it. This is simple, fast, and requires no explicit binding.

### **Type 2: Reactive Structure (The "Bind" Model)**

* **What it's for:** Changing the *structure* of the UI (e.g., adding or removing items from a list in a ScrollContainer). This is for re-composing a *subset* of the tree.  
* **How it works:** This is *only* for container elements. During composition, inside the container's scope lambda (e.g., col -> { ... }), you simply call **col.bind(myState)**.  
* **Data Flow:**  
  1. The col.bind(myState) call acts as a **marker**. It tells the framework: "Store the *entire* lambda function for this scope (e.g., the col -> { ... } block) and associate it with myState."  
  2. The framework then continues to execute the rest of the lambda *once* to build the initial static content for that scope.  
  3. When you later call .set() on myState, the framework is notified. It finds all the lambdas bound to that state, clears all children from their respective containers, and **re-executes the entire lambda** to build a new set of children.