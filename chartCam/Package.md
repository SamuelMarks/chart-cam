# Package io.healthplatform.chartcam

Provides the core application entry points, platform-specific initializers, and top-level definitions for the ChartCam application. 

This package is the architectural root of the project and is responsible for:
- Orchestrating the `App` composable, which serves as the top-level container of the Compose UI hierarchy.
- Defining the dependency graph, resolving singleton components such as repositories, databases, and configuration managers.
- Providing routing logic and navigation graphs across the application's screens (e.g., login, patient list, encounter details).
- Defining cross-cutting abstractions and high-level configurations.
