# 📖 Project Documentation

## 🔗 Module Dependencies
Below is a diagram representing the module dependencies in this project. Arrows indicate dependencies.
For example: **Network** → **Data** means **Data** depends on **Network**, or in other words, **Network**
code is accessible within the **Data** module.

```mermaid
---
config:
  theme: dark
---
flowchart TD
    A[App]
    D[Data]
    S[Datastore]
    N[Network]
    B[Database]
    U[UI]

    D --> U
    U --> A
    D --> A

    subgraph Core
    B --> D
    S --> D
    N --> D
    end
```


## 🏷️ Naming & Coding Convention
#### Each screen can have their own repository to retrieve data.
```kotlin
HomeScreen.kt
HomeRepository.kt
```

#### There are 2 types of repositories
   * Related to Screen `HomeRepository.kt`
   * Related to Data `ProgressRepo.kt`

More documentation will be added in the future.

