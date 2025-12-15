#### ## Overview
A **Hierarchy Tree Class** represents a structured tree of nodes for organizing and managing inverted images in
the iBrowserImage Inverter project. This is particularly relevant for Java backend data modeling and React
frontend UI representation.

- The core class should be `Tree` (managing root node, children, etc.), with child classes like `Node` handling
individual image/inversion data.
- Relationships are imposed via UML constructs: e.g., parent-child associations using composition or containment
to ensure logical integrity in both backend and frontend implementations.

#### ## Core Classes
Here's a minimal Java class definition for the hierarchy tree. Assume nodes store basic image metadata; more
details (e.g., inversion parameters) can be added if specified:
