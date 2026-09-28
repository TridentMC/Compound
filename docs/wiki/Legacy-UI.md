The UI module is designed to ease in the design and implementation of user interfaces. It provides three main ways of interacting with the game's screen: Elements, Layouts, and the ScreenContext.

Compound provides the following Elements out of the box:
- **Box** - A standard grey box, matching the design of the backgrounds of vanilla GUIs.
- **Image** - A way to draw a static image on the screen, supply a resource location and the UVs and it deals with the rest.
- **Label** - Used to draw text on the screen, supports multi line formatted text.
- **Button** - A button, provides hooks for hovering and clicking.
- **Slot (Containers Only)** - Instead of special casing slots like the base game all of the slot functionality is wrapped into an element.

To place these elements on the screen there's also the following layouts already implemented:
- **Centered** - Center an element on the screen, vertically and/or horizontally.
- **Grid** - Positions elements in a grid, elements can also be dynamically added or removed.
- **Multi** - Allows chaining of different layouts together, so you can center a grid of elements relative to another.
- **None** - No layout, just the raw element positioning will be used.
- **Relative** - Place an element relative to the position of another element.
- **Marquee** - Remember the Marquee tag? I do. This is effectively just an example of how you can implement animations using the Layout system.

Custom elements and layouts can be added by extending the base classes in the UI module. Elements use the ScreenContext to interact with the screen, all of the methods inside the ScreenContext contain JavaDocs explaining how each method works.