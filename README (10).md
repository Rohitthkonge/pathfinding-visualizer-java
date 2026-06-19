# Pathfinding Visualizer (Dijkstra & A*)

A Java Swing app that visually animates how **Dijkstra's algorithm** and **A\*** search a grid to find the shortest path between two points, dodging walls you draw yourself.

## How to Use

1. **Click** any cell → sets the **START** point (green)
2. **Click** another cell → sets the **END** point (red)
3. **Click or drag** to draw **walls** (dark cells the path can't cross)
4. Click **Run Dijkstra** or **Run A\*** to watch it search
5. **Clear Path** — keeps your walls, resets the search so you can re-run
6. **Reset Grid** — wipes everything and starts over

## Color Key

| Color | Meaning |
|---|---|
| 🟩 Green | Start |
| 🟥 Red | End |
| ⬛ Dark | Wall |
| 🟦 Light blue | Visited during search |
| 🟪 Purple | Final shortest path |

## How It Works

- The grid is a 2D array of `Cell` objects, each tracking its position, wall status, distance, and the cell it came from (for path reconstruction).
- **Dijkstra** explores cells in order of distance from start using a priority queue — guaranteed shortest path, but explores in all directions equally.
- **A\*** does the same thing but adds a heuristic (Manhattan distance to the end) so it's biased to search *toward* the goal — usually visits far fewer cells than Dijkstra for the same result.
- Both algorithms record the order cells were visited, which is then animated with a `Swing Timer` so you can see the search happen step by step.
- Once the end is reached, the path is reconstructed by walking backward through each cell's `previous` pointer, then animated in purple.

## Requirements

- Java JDK 8 or above

## How to Run

```bash
javac PathfindingVisualizer.java
java PathfindingVisualizer
```

## Possible Improvements

- Add diagonal movement
- Add weighted terrain (e.g., "mud" cells that cost more to cross)
- Add Breadth-First Search and Depth-First Search as extra comparison modes
- Show live stats (cells visited, path length) on screen instead of only in the status bar

## Author

Rohitth
