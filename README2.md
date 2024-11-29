### Module Graph

```mermaid
%%{
  init: {
    'theme': 'neutral'
  }
}%%

graph LR
  subgraph :annotation
    :annotation:annotation-sampled["annotation-sampled"]
    :annotation:annotation["annotation"]
  end
  subgraph :collection
    :collection:collection["collection"]
  end
  subgraph :compose
    :compose:test-utils["test-utils"]
    :compose:mpp["mpp"]
  end
  subgraph :compose:animation
    :compose:animation:animation["animation"]
    :compose:animation:animation-core-lint["animation-core-lint"]
    :compose:animation:animation-tooling-internal["animation-tooling-internal"]
    :compose:animation:animation-graphics["animation-graphics"]
    :compose:animation:animation-core["animation-core"]
    :compose:animation:animation-lint["animation-lint"]
  end
  subgraph :compose:animation:animation
    :compose:animation:animation:animation-samples["animation-samples"]
  end
  subgraph :compose:animation:animation-core
    :compose:animation:animation-core:animation-core-samples["animation-core-samples"]
  end
  subgraph :compose:animation:animation-graphics
    :compose:animation:animation-graphics:animation-graphics-samples["animation-graphics-samples"]
  end
  subgraph :compose:animation:animation:integration-tests
    :compose:animation:animation:integration-tests:animation-demos["animation-demos"]
  end
  subgraph :compose:desktop
    :compose:desktop:desktop["desktop"]
  end
  subgraph :compose:desktop:desktop
    :compose:desktop:desktop:desktop-samples["desktop-samples"]
    :compose:desktop:desktop:desktop-samples-material3["desktop-samples-material3"]
  end
  subgraph :compose:foundation
    :compose:foundation:foundation-layout["foundation-layout"]
    :compose:foundation:foundation["foundation"]
    :compose:foundation:foundation-lint["foundation-lint"]
  end
  subgraph :compose:foundation:foundation
    :compose:foundation:foundation:foundation-samples["foundation-samples"]
  end
  subgraph :compose:foundation:foundation-layout
    :compose:foundation:foundation-layout:foundation-layout-samples["foundation-layout-samples"]
  end
  subgraph :compose:foundation:foundation-layout:integration-tests
    :compose:foundation:foundation-layout:integration-tests:foundation-layout-demos["foundation-layout-demos"]
  end
  subgraph :compose:foundation:foundation:integration-tests
    :compose:foundation:foundation:integration-tests:foundation-demos["foundation-demos"]
  end
  subgraph :compose:integration-tests
    :compose:integration-tests:docs-snippets["docs-snippets"]
    :compose:integration-tests:demos["demos"]
    :compose:integration-tests:material-catalog["material-catalog"]
  end
  subgraph :compose:integration-tests:demos
    :compose:integration-tests:demos:common["common"]
  end
  subgraph :compose:lint
    :compose:lint:common["common"]
    :compose:lint:common-test["common-test"]
    :compose:lint:internal-lint-checks["internal-lint-checks"]
  end
  subgraph :compose:material
    :compose:material:material-lint["material-lint"]
    :compose:material:material["material"]
    :compose:material:material-navigation["material-navigation"]
    :compose:material:material-ripple["material-ripple"]
  end
  subgraph :compose:material3
    :compose:material3:material3["material3"]
    :compose:material3:material3-adaptive-navigation-suite["material3-adaptive-navigation-suite"]
    :compose:material3:material3-window-size-class["material3-window-size-class"]
    :compose:material3:material3-lint["material3-lint"]
    :compose:material3:material3-common["material3-common"]
  end
  subgraph :compose:material3:adaptive
    :compose:material3:adaptive:adaptive["adaptive"]
    :compose:material3:adaptive:adaptive-layout["adaptive-layout"]
    :compose:material3:adaptive:adaptive-navigation["adaptive-navigation"]
  end
  subgraph :compose:material3:material3
    :compose:material3:material3:material3-samples["material3-samples"]
  end
  subgraph :compose:material3:material3-window-size-class
    :compose:material3:material3-window-size-class:material3-window-size-class-samples["material3-window-size-class-samples"]
  end
  subgraph :compose:material:material
    :compose:material:material:material-samples["material-samples"]
  end
  subgraph :compose:mpp
    :compose:mpp:demo-uikit["demo-uikit"]
    :compose:mpp:demo["demo"]
  end
  subgraph :compose:runtime
    :compose:runtime:runtime["runtime"]
    :compose:runtime:runtime-lint["runtime-lint"]
    :compose:runtime:runtime-livedata["runtime-livedata"]
    :compose:runtime:runtime-saveable-lint["runtime-saveable-lint"]
    :compose:runtime:runtime-test-utils["runtime-test-utils"]
    :compose:runtime:runtime-saveable["runtime-saveable"]
    :compose:runtime:runtime-rxjava3["runtime-rxjava3"]
    :compose:runtime:runtime-rxjava2["runtime-rxjava2"]
    :compose:runtime:runtime-tracing["runtime-tracing"]
  end
  subgraph :compose:runtime:runtime
    :compose:runtime:runtime:integration-tests["integration-tests"]
    :compose:runtime:runtime:runtime-samples["runtime-samples"]
  end
  subgraph :compose:runtime:runtime-livedata
    :compose:runtime:runtime-livedata:runtime-livedata-samples["runtime-livedata-samples"]
  end
  subgraph :compose:runtime:runtime-rxjava2
    :compose:runtime:runtime-rxjava2:runtime-rxjava2-samples["runtime-rxjava2-samples"]
  end
  subgraph :compose:runtime:runtime-rxjava3
    :compose:runtime:runtime-rxjava3:runtime-rxjava3-samples["runtime-rxjava3-samples"]
  end
  subgraph :compose:runtime:runtime-saveable
    :compose:runtime:runtime-saveable:runtime-saveable-samples["runtime-saveable-samples"]
  end
  subgraph :compose:ui
    :compose:ui:ui-graphics-lint["ui-graphics-lint"]
    :compose:ui:ui-tooling["ui-tooling"]
    :compose:ui:ui-tooling-preview["ui-tooling-preview"]
    :compose:ui:ui["ui"]
    :compose:ui:ui-test-manifest-lint["ui-test-manifest-lint"]
    :compose:ui:ui-test["ui-test"]
    :compose:ui:ui-test-junit4["ui-test-junit4"]
    :compose:ui:ui-graphics["ui-graphics"]
    :compose:ui:ui-text["ui-text"]
    :compose:ui:ui-unit["ui-unit"]
    :compose:ui:ui-util["ui-util"]
    :compose:ui:ui-lint["ui-lint"]
    :compose:ui:ui-uikit["ui-uikit"]
    :compose:ui:ui-tooling-data["ui-tooling-data"]
    :compose:ui:ui-test-manifest["ui-test-manifest"]
    :compose:ui:ui-viewbinding["ui-viewbinding"]
    :compose:ui:ui-geometry["ui-geometry"]
    :compose:ui:ui-android-stubs["ui-android-stubs"]
    :compose:ui:ui-text-google-fonts["ui-text-google-fonts"]
  end
  subgraph :compose:ui:ui
    :compose:ui:ui:ui-samples["ui-samples"]
  end
  subgraph :compose:ui:ui-graphics
    :compose:ui:ui-graphics:ui-graphics-samples["ui-graphics-samples"]
  end
  subgraph :compose:ui:ui-test
    :compose:ui:ui-test:ui-test-samples["ui-test-samples"]
  end
  subgraph :compose:ui:ui-test-manifest:integration-tests
    :compose:ui:ui-test-manifest:integration-tests:testapp["testapp"]
  end
  subgraph :compose:ui:ui-text
    :compose:ui:ui-text:ui-text-samples["ui-text-samples"]
  end
  subgraph :compose:ui:ui-unit
    :compose:ui:ui-unit:ui-unit-samples["ui-unit-samples"]
  end
  subgraph :compose:ui:ui-viewbinding
    :compose:ui:ui-viewbinding:ui-viewbinding-samples["ui-viewbinding-samples"]
  end
  subgraph :compose:ui:ui:integration-tests
    :compose:ui:ui:integration-tests:ui-demos["ui-demos"]
  end
  subgraph :core
    :core:core-bundle["core-bundle"]
    :core:core-uri["core-uri"]
  end
  subgraph :graphics
    :graphics:graphics-shapes["graphics-shapes"]
  end
  subgraph :kruth
    :kruth:kruth["kruth"]
  end
  subgraph :lifecycle
    :lifecycle:lifecycle-runtime-compose["lifecycle-runtime-compose"]
    :lifecycle:lifecycle-runtime["lifecycle-runtime"]
    :lifecycle:lifecycle-common["lifecycle-common"]
    :lifecycle:lifecycle-runtime-testing["lifecycle-runtime-testing"]
    :lifecycle:lifecycle-viewmodel["lifecycle-viewmodel"]
    :lifecycle:lifecycle-viewmodel-savedstate["lifecycle-viewmodel-savedstate"]
    :lifecycle:lifecycle-runtime-testing-lint["lifecycle-runtime-testing-lint"]
    :lifecycle:lifecycle-viewmodel-compose["lifecycle-viewmodel-compose"]
    :lifecycle:lifecycle-runtime-lint["lifecycle-runtime-lint"]
  end
  subgraph :lint-checks
    :lint-checks:integration-tests["integration-tests"]
  end
  subgraph :navigation
    :navigation:navigation-testing["navigation-testing"]
    :navigation:navigation-runtime["navigation-runtime"]
    :navigation:navigation-common["navigation-common"]
    :navigation:navigation-compose["navigation-compose"]
  end
  subgraph :savedstate
    :savedstate:savedstate["savedstate"]
  end
  subgraph :window
    :window:window-core["window-core"]
  end
  :compose:ui:ui-graphics-lint --> :compose:lint:common
  :compose:ui:ui-graphics-lint --> :lint-checks
  :compose:ui:ui-graphics-lint --> :compose:lint:common-test
  :compose:material3:material3:material3-samples --> :annotation:annotation-sampled
  :compose:material3:material3:material3-samples --> :compose:ui:ui-tooling
  :compose:material3:material3:material3-samples --> :compose:foundation:foundation-layout
  :compose:material3:material3:material3-samples --> :compose:material3:material3
  :compose:material3:material3:material3-samples --> :compose:ui:ui-tooling-preview
  :compose:material3:material3:material3-samples --> :compose:lint:internal-lint-checks
  :compose:material3:material3:material3-samples --> :lint-checks
  :compose:animation:animation:animation-samples --> :annotation:annotation-sampled
  :compose:animation:animation:animation-samples --> :compose:animation:animation
  :compose:animation:animation:animation-samples --> :compose:lint:internal-lint-checks
  :compose:animation:animation:animation-samples --> :lint-checks
  :compose:material:material-lint --> :compose:lint:common
  :compose:material:material-lint --> :lint-checks
  :compose:material:material-lint --> :compose:lint:common-test
  :compose:foundation:foundation:foundation-samples --> :annotation:annotation-sampled
  :compose:foundation:foundation:foundation-samples --> :compose:ui:ui-tooling
  :compose:foundation:foundation:foundation-samples --> :compose:foundation:foundation
  :compose:foundation:foundation:foundation-samples --> :compose:foundation:foundation-layout
  :compose:foundation:foundation:foundation-samples --> :compose:lint:internal-lint-checks
  :compose:foundation:foundation:foundation-samples --> :lint-checks
  :compose:material3:material3-adaptive-navigation-suite --> :compose:test-utils
  :compose:material3:material3-adaptive-navigation-suite --> :annotation:annotation
  :compose:material3:material3-adaptive-navigation-suite --> :compose:material3:material3
  :compose:material3:material3-adaptive-navigation-suite --> :compose:material3:adaptive:adaptive
  :compose:material3:material3-adaptive-navigation-suite --> :compose:ui:ui
  :compose:material3:material3-adaptive-navigation-suite --> :window:window-core
  :compose:material3:material3-adaptive-navigation-suite --> :kruth:kruth
  :compose:material3:material3-adaptive-navigation-suite --> :compose:lint:internal-lint-checks
  :compose:material3:material3-adaptive-navigation-suite --> :lint-checks
  :compose:desktop:desktop:desktop-samples --> :compose:desktop:desktop
  :compose:desktop:desktop:desktop-samples --> :lint-checks
  :lifecycle:lifecycle-runtime-compose --> :annotation:annotation
  :lifecycle:lifecycle-runtime-compose --> :lifecycle:lifecycle-runtime
  :lifecycle:lifecycle-runtime-compose --> :compose:runtime:runtime
  :lifecycle:lifecycle-runtime-compose --> :lifecycle:lifecycle-common
  :lifecycle:lifecycle-runtime-compose --> :compose:lint:internal-lint-checks
  :lifecycle:lifecycle-runtime-compose --> :lint-checks
  :compose:material3:material3-window-size-class:material3-window-size-class-samples --> :annotation:annotation-sampled
  :compose:material3:material3-window-size-class:material3-window-size-class-samples --> :compose:material3:material3-window-size-class
  :compose:material3:material3-window-size-class:material3-window-size-class-samples --> :compose:lint:internal-lint-checks
  :compose:material3:material3-window-size-class:material3-window-size-class-samples --> :lint-checks
  :internal-testutils-mockito --> :lint-checks
  :navigation:navigation-testing --> :navigation:navigation-runtime
  :navigation:navigation-testing --> :lifecycle:lifecycle-runtime-testing
  :navigation:navigation-testing --> :internal-testutils-navigation
  :navigation:navigation-testing --> :kruth:kruth
  :navigation:navigation-testing --> :lint-checks
  :compose:runtime:runtime-lint --> :compose:lint:common
  :compose:runtime:runtime-lint --> :lint-checks
  :compose:runtime:runtime-lint --> :compose:lint:common-test
  :compose:ui:ui-test-manifest-lint --> :lint-checks
  :compose:ui:ui-tooling-preview --> :compose:runtime:runtime
  :compose:ui:ui-tooling-preview --> :compose:lint:internal-lint-checks
  :compose:ui:ui-tooling-preview --> :lint-checks
  :compose:material3:material3-lint --> :compose:lint:common
  :compose:material3:material3-lint --> :lint-checks
  :compose:material3:material3-lint --> :compose:lint:common-test
  :compose:ui:ui-test --> :compose:material:material
  :compose:ui:ui-test --> :compose:ui:ui-test-junit4
  :compose:ui:ui-test --> :compose:test-utils
  :compose:ui:ui-test --> :compose:ui:ui-graphics
  :compose:ui:ui-test --> :compose:runtime:runtime
  :compose:ui:ui-test --> :compose:ui:ui
  :compose:ui:ui-test --> :compose:ui:ui-text
  :compose:ui:ui-test --> :compose:ui:ui-unit
  :compose:ui:ui-test --> :annotation:annotation
  :compose:ui:ui-test --> :collection:collection
  :compose:ui:ui-test --> :compose:ui:ui-util
  :compose:ui:ui-test --> :compose:lint:internal-lint-checks
  :compose:ui:ui-test --> :lint-checks
  :compose:ui:ui-test --> :compose:ui:ui-test:ui-test-samples
  :compose:ui:ui-test --> :compose:foundation:foundation
  :compose:foundation:foundation-layout:integration-tests:foundation-layout-demos --> :compose:foundation:foundation
  :compose:foundation:foundation-layout:integration-tests:foundation-layout-demos --> :compose:foundation:foundation-layout
  :compose:foundation:foundation-layout:integration-tests:foundation-layout-demos --> :compose:foundation:foundation-layout:foundation-layout-samples
  :compose:foundation:foundation-layout:integration-tests:foundation-layout-demos --> :compose:material:material
  :compose:foundation:foundation-layout:integration-tests:foundation-layout-demos --> :compose:integration-tests:demos:common
  :compose:foundation:foundation-layout:integration-tests:foundation-layout-demos --> :compose:runtime:runtime
  :compose:foundation:foundation-layout:integration-tests:foundation-layout-demos --> :compose:ui:ui
  :compose:foundation:foundation-layout:integration-tests:foundation-layout-demos --> :compose:ui:ui-text
  :compose:foundation:foundation-layout:integration-tests:foundation-layout-demos --> :compose:lint:internal-lint-checks
  :compose:foundation:foundation-layout:integration-tests:foundation-layout-demos --> :lint-checks
  :navigation:navigation-common --> :annotation:annotation
  :navigation:navigation-common --> :collection:collection
  :navigation:navigation-common --> :core:core-bundle
  :navigation:navigation-common --> :core:core-uri
  :navigation:navigation-common --> :lifecycle:lifecycle-common
  :navigation:navigation-common --> :lifecycle:lifecycle-runtime
  :navigation:navigation-common --> :lifecycle:lifecycle-viewmodel
  :navigation:navigation-common --> :lifecycle:lifecycle-viewmodel-savedstate
  :navigation:navigation-common --> :savedstate:savedstate
  :navigation:navigation-common --> :kruth:kruth
  :navigation:navigation-common --> :navigation:navigation-testing
  :navigation:navigation-common --> :lint-checks
  :compose:foundation:foundation-lint --> :compose:lint:common
  :compose:foundation:foundation-lint --> :lint-checks
  :compose:foundation:foundation-lint --> :compose:lint:common-test
  :compose:ui:ui-lint --> :compose:lint:common
  :compose:ui:ui-lint --> :lint-checks
  :compose:ui:ui-lint --> :compose:lint:common-test
  :compose:animation:animation-core-lint --> :compose:lint:common
  :compose:animation:animation-core-lint --> :lint-checks
  :compose:animation:animation-core-lint --> :compose:lint:common-test
  :compose:ui:ui-util --> :collection:collection
  :compose:ui:ui-util --> :compose:lint:internal-lint-checks
  :compose:ui:ui-util --> :lint-checks
  :compose:ui:ui-util --> :compose:ui:ui-uikit
  :compose:ui:ui-tooling --> :compose:ui:ui-test-junit4
  :compose:ui:ui-tooling --> :compose:foundation:foundation-layout
  :compose:ui:ui-tooling --> :compose:foundation:foundation
  :compose:ui:ui-tooling --> :compose:test-utils
  :compose:ui:ui-tooling --> :compose:animation:animation-tooling-internal
  :compose:ui:ui-tooling --> :compose:runtime:runtime-livedata
  :compose:ui:ui-tooling --> :compose:ui:ui-tooling-preview
  :compose:ui:ui-tooling --> :compose:runtime:runtime
  :compose:ui:ui-tooling --> :compose:ui:ui
  :compose:ui:ui-tooling --> :compose:ui:ui-tooling-data
  :compose:ui:ui-tooling --> :compose:animation:animation
  :compose:ui:ui-tooling --> :compose:lint:internal-lint-checks
  :compose:ui:ui-tooling --> :lint-checks
  :internal-testutils-common --> :lint-checks
  :lifecycle:lifecycle-viewmodel-savedstate --> :annotation:annotation
  :lifecycle:lifecycle-viewmodel-savedstate --> :core:core-bundle
  :lifecycle:lifecycle-viewmodel-savedstate --> :lifecycle:lifecycle-common
  :lifecycle:lifecycle-viewmodel-savedstate --> :lifecycle:lifecycle-viewmodel
  :lifecycle:lifecycle-viewmodel-savedstate --> :savedstate:savedstate
  :lifecycle:lifecycle-viewmodel-savedstate --> :lifecycle:lifecycle-runtime
  :lifecycle:lifecycle-viewmodel-savedstate --> :internal-testutils-runtime
  :lifecycle:lifecycle-viewmodel-savedstate --> :lint-checks
  :compose:animation:animation-graphics:animation-graphics-samples --> :annotation:annotation-sampled
  :compose:animation:animation-graphics:animation-graphics-samples --> :compose:animation:animation
  :compose:animation:animation-graphics:animation-graphics-samples --> :compose:animation:animation-graphics
  :compose:animation:animation-graphics:animation-graphics-samples --> :compose:lint:internal-lint-checks
  :compose:animation:animation-graphics:animation-graphics-samples --> :lint-checks
  :compose:runtime:runtime-saveable-lint --> :compose:lint:common
  :compose:runtime:runtime-saveable-lint --> :lint-checks
  :compose:runtime:runtime-saveable-lint --> :compose:lint:common-test
  :compose:material:material-navigation --> :compose:test-utils
  :compose:material:material-navigation --> :navigation:navigation-testing
  :compose:material:material-navigation --> :compose:ui:ui-test-junit4
  :compose:material:material-navigation --> :compose:ui:ui-test-manifest
  :compose:material:material-navigation --> :navigation:navigation-compose
  :compose:material:material-navigation --> :compose:material:material
  :compose:material:material-navigation --> :compose:lint:internal-lint-checks
  :compose:material:material-navigation --> :lint-checks
  :compose:runtime:runtime:integration-tests --> :compose:ui:ui
  :compose:runtime:runtime:integration-tests --> :compose:material:material
  :compose:runtime:runtime:integration-tests --> :compose:ui:ui-test-junit4
  :compose:runtime:runtime:integration-tests --> :compose:test-utils
  :compose:runtime:runtime:integration-tests --> :compose:lint:internal-lint-checks
  :compose:runtime:runtime:integration-tests --> :lint-checks
  :compose:integration-tests:docs-snippets --> :compose:animation:animation-graphics
  :compose:integration-tests:docs-snippets --> :compose:foundation:foundation-layout
  :compose:integration-tests:docs-snippets --> :compose:material:material
  :compose:integration-tests:docs-snippets --> :compose:material3:material3
  :compose:integration-tests:docs-snippets --> :compose:runtime:runtime
  :compose:integration-tests:docs-snippets --> :compose:runtime:runtime-livedata
  :compose:integration-tests:docs-snippets --> :compose:ui:ui-graphics
  :compose:integration-tests:docs-snippets --> :compose:ui:ui-test-junit4
  :compose:integration-tests:docs-snippets --> :compose:ui:ui-tooling-preview
  :compose:integration-tests:docs-snippets --> :compose:ui:ui-viewbinding
  :compose:integration-tests:docs-snippets --> :compose:lint:internal-lint-checks
  :compose:integration-tests:docs-snippets --> :lint-checks
  :compose:foundation:foundation-layout --> :compose:foundation:foundation
  :compose:foundation:foundation-layout --> :compose:ui:ui-test-junit4
  :compose:foundation:foundation-layout --> :compose:test-utils
  :compose:foundation:foundation-layout --> :compose:ui:ui
  :compose:foundation:foundation-layout --> :collection:collection
  :compose:foundation:foundation-layout --> :compose:runtime:runtime
  :compose:foundation:foundation-layout --> :compose:ui:ui-util
  :compose:foundation:foundation-layout --> :annotation:annotation
  :compose:foundation:foundation-layout --> :compose:lint:internal-lint-checks
  :compose:foundation:foundation-layout --> :lint-checks
  :compose:foundation:foundation-layout --> :compose:foundation:foundation-layout:foundation-layout-samples
  :compose:animation:animation-core:animation-core-samples --> :annotation:annotation-sampled
  :compose:animation:animation-core:animation-core-samples --> :compose:animation:animation-core
  :compose:animation:animation-core:animation-core-samples --> :compose:lint:internal-lint-checks
  :compose:animation:animation-core:animation-core-samples --> :lint-checks
  :window:window-core --> :annotation:annotation
  :window:window-core --> :lint-checks
  :compose:ui:ui-unit:ui-unit-samples --> :annotation:annotation-sampled
  :compose:ui:ui-unit:ui-unit-samples --> :compose:ui:ui
  :compose:ui:ui-unit:ui-unit-samples --> :compose:ui:ui-unit
  :compose:ui:ui-unit:ui-unit-samples --> :compose:lint:internal-lint-checks
  :compose:ui:ui-unit:ui-unit-samples --> :lint-checks
  :compose:integration-tests:demos --> :compose:animation:animation:integration-tests:animation-demos
  :compose:integration-tests:demos --> :compose:foundation:foundation-layout:integration-tests:foundation-layout-demos
  :compose:integration-tests:demos --> :compose:foundation:foundation:integration-tests:foundation-demos
  :compose:integration-tests:demos --> :compose:ui:ui:integration-tests:ui-demos
  :compose:integration-tests:demos --> :compose:foundation:foundation
  :compose:integration-tests:demos --> :compose:foundation:foundation-layout
  :compose:integration-tests:demos --> :compose:integration-tests:demos:common
  :compose:integration-tests:demos --> :compose:material:material
  :compose:integration-tests:demos --> :compose:material3:material3
  :compose:integration-tests:demos --> :compose:runtime:runtime
  :compose:integration-tests:demos --> :compose:ui:ui
  :compose:integration-tests:demos --> :compose:lint:internal-lint-checks
  :compose:integration-tests:demos --> :lint-checks
  :compose:lint:internal-lint-checks --> :compose:lint:common
  :compose:lint:internal-lint-checks --> :lint-checks
  :compose:lint:internal-lint-checks --> :compose:lint:common-test
  :compose:integration-tests:material-catalog --> :compose:runtime:runtime
  :compose:integration-tests:material-catalog --> :compose:foundation:foundation-layout
  :compose:integration-tests:material-catalog --> :compose:ui:ui
  :compose:integration-tests:material-catalog --> :compose:material:material
  :compose:integration-tests:material-catalog --> :compose:material3:material3
  :compose:integration-tests:material-catalog --> :compose:lint:internal-lint-checks
  :compose:integration-tests:material-catalog --> :lint-checks
  :compose:ui:ui:integration-tests:ui-demos --> :compose:ui:ui-tooling
  :compose:ui:ui:integration-tests:ui-demos --> :compose:animation:animation
  :compose:ui:ui:integration-tests:ui-demos --> :compose:foundation:foundation
  :compose:ui:ui:integration-tests:ui-demos --> :compose:foundation:foundation-layout
  :compose:ui:ui:integration-tests:ui-demos --> :compose:integration-tests:demos:common
  :compose:ui:ui:integration-tests:ui-demos --> :compose:ui:ui:ui-samples
  :compose:ui:ui:integration-tests:ui-demos --> :compose:material:material
  :compose:ui:ui:integration-tests:ui-demos --> :compose:runtime:runtime
  :compose:ui:ui:integration-tests:ui-demos --> :compose:runtime:runtime-livedata
  :compose:ui:ui:integration-tests:ui-demos --> :compose:ui:ui
  :compose:ui:ui:integration-tests:ui-demos --> :compose:ui:ui-text
  :compose:ui:ui:integration-tests:ui-demos --> :compose:ui:ui-tooling-preview
  :compose:ui:ui:integration-tests:ui-demos --> :compose:ui:ui-viewbinding
  :compose:ui:ui:integration-tests:ui-demos --> :compose:lint:internal-lint-checks
  :compose:ui:ui:integration-tests:ui-demos --> :lint-checks
  :lifecycle:lifecycle-runtime-testing-lint --> :compose:lint:common
  :lifecycle:lifecycle-runtime-testing-lint --> :lint-checks
  :lifecycle:lifecycle-runtime-testing-lint --> :compose:lint:common-test
  :internal-testutils-ktx --> :lint-checks
  :compose:runtime:runtime-test-utils --> :compose:runtime:runtime
  :compose:runtime:runtime-test-utils --> :compose:lint:internal-lint-checks
  :compose:runtime:runtime-test-utils --> :lint-checks
  :compose:material:material --> :compose:material:material:material-samples
  :compose:material:material --> :compose:test-utils
  :compose:material:material --> :compose:animation:animation-core
  :compose:material:material --> :compose:foundation:foundation
  :compose:material:material --> :compose:material:material-ripple
  :compose:material:material --> :compose:runtime:runtime
  :compose:material:material --> :compose:ui:ui
  :compose:material:material --> :compose:ui:ui-text
  :compose:material:material --> :compose:animation:animation
  :compose:material:material --> :compose:foundation:foundation-layout
  :compose:material:material --> :compose:ui:ui-util
  :compose:material:material --> :annotation:annotation
  :compose:material:material --> :compose:ui:ui-test
  :compose:material:material --> :compose:ui:ui-test-junit4
  :compose:material:material --> :compose:lint:internal-lint-checks
  :compose:material:material --> :lint-checks
  :compose:ui:ui-tooling-data --> :compose:ui:ui-test-junit4
  :compose:ui:ui-tooling-data --> :compose:foundation:foundation-layout
  :compose:ui:ui-tooling-data --> :compose:foundation:foundation
  :compose:ui:ui-tooling-data --> :compose:material:material
  :compose:ui:ui-tooling-data --> :compose:runtime:runtime
  :compose:ui:ui-tooling-data --> :compose:ui:ui
  :compose:ui:ui-tooling-data --> :compose:lint:internal-lint-checks
  :compose:ui:ui-tooling-data --> :lint-checks
  :compose:ui:ui-tooling-data --> :compose:ui:ui-unit:ui-unit-samples
  :compose:ui:ui-unit --> :compose:ui:ui-geometry
  :compose:ui:ui-unit --> :compose:runtime:runtime
  :compose:ui:ui-unit --> :compose:ui:ui-util
  :compose:ui:ui-unit --> :annotation:annotation
  :compose:ui:ui-unit --> :compose:lint:internal-lint-checks
  :compose:ui:ui-unit --> :lint-checks
  :compose:ui:ui-unit --> :compose:ui:ui-unit:ui-unit-samples
  :compose:ui:ui-text --> :compose:ui:ui-test-junit4
  :compose:ui:ui-text --> :internal-testutils-fonts
  :compose:ui:ui-text --> :compose:ui:ui-graphics
  :compose:ui:ui-text --> :compose:ui:ui-unit
  :compose:ui:ui-text --> :compose:runtime:runtime
  :compose:ui:ui-text --> :compose:runtime:runtime-saveable
  :compose:ui:ui-text --> :compose:ui:ui-util
  :compose:ui:ui-text --> :compose:ui:ui-geometry
  :compose:ui:ui-text --> :collection:collection
  :compose:ui:ui-text --> :annotation:annotation
  :compose:ui:ui-text --> :compose:foundation:foundation
  :compose:ui:ui-text --> :compose:lint:internal-lint-checks
  :compose:ui:ui-text --> :lint-checks
  :compose:ui:ui-text --> :compose:ui:ui-text:ui-text-samples
  :compose:ui:ui-viewbinding --> :compose:foundation:foundation
  :compose:ui:ui-viewbinding --> :compose:test-utils
  :compose:ui:ui-viewbinding --> :compose:ui:ui
  :compose:ui:ui-viewbinding --> :compose:ui:ui-util
  :compose:ui:ui-viewbinding --> :compose:lint:internal-lint-checks
  :compose:ui:ui-viewbinding --> :lint-checks
  :compose:ui:ui-viewbinding --> :compose:ui:ui-viewbinding:ui-viewbinding-samples
  :compose:ui:ui:ui-samples --> :annotation:annotation-sampled
  :compose:ui:ui:ui-samples --> :compose:ui:ui
  :compose:ui:ui:ui-samples --> :compose:lint:internal-lint-checks
  :compose:ui:ui:ui-samples --> :lint-checks
  :compose:runtime:runtime-livedata:runtime-livedata-samples --> :annotation:annotation-sampled
  :compose:runtime:runtime-livedata:runtime-livedata-samples --> :compose:runtime:runtime-livedata
  :compose:runtime:runtime-livedata:runtime-livedata-samples --> :compose:lint:internal-lint-checks
  :compose:runtime:runtime-livedata:runtime-livedata-samples --> :lint-checks
  :compose:mpp:demo-uikit --> :compose:foundation:foundation
  :compose:mpp:demo-uikit --> :compose:foundation:foundation-layout
  :compose:mpp:demo-uikit --> :compose:material:material
  :compose:mpp:demo-uikit --> :compose:mpp
  :compose:mpp:demo-uikit --> :compose:mpp:demo
  :compose:mpp:demo-uikit --> :compose:runtime:runtime
  :compose:mpp:demo-uikit --> :compose:ui:ui
  :compose:mpp:demo-uikit --> :compose:ui:ui-graphics
  :compose:mpp:demo-uikit --> :compose:ui:ui-text
  :kruth:kruth --> :lint-checks
  :compose:ui:ui-test-junit4 --> :compose:animation:animation
  :compose:ui:ui-test-junit4 --> :compose:test-utils
  :compose:ui:ui-test-junit4 --> :compose:material:material
  :compose:ui:ui-test-junit4 --> :compose:ui:ui-test
  :compose:ui:ui-test-junit4 --> :compose:runtime:runtime-saveable
  :compose:ui:ui-test-junit4 --> :annotation:annotation
  :compose:ui:ui-test-junit4 --> :compose:animation:animation-core
  :compose:ui:ui-test-junit4 --> :compose:foundation:foundation
  :compose:ui:ui-test-junit4 --> :compose:lint:internal-lint-checks
  :compose:ui:ui-test-junit4 --> :lint-checks
  :compose:animation:animation-core --> :compose:animation:animation
  :compose:animation:animation-core --> :compose:ui:ui-test-junit4
  :compose:animation:animation-core --> :compose:test-utils
  :compose:animation:animation-core --> :collection:collection
  :compose:animation:animation-core --> :compose:runtime:runtime
  :compose:animation:animation-core --> :compose:ui:ui
  :compose:animation:animation-core --> :compose:ui:ui-unit
  :compose:animation:animation-core --> :compose:ui:ui-util
  :compose:animation:animation-core --> :annotation:annotation
  :compose:animation:animation-core --> :kruth:kruth
  :compose:animation:animation-core --> :compose:lint:internal-lint-checks
  :compose:animation:animation-core --> :lint-checks
  :compose:animation:animation-core --> :compose:animation:animation-core:animation-core-samples
  :compose:animation:animation:integration-tests:animation-demos --> :compose:ui:ui-tooling
  :compose:animation:animation:integration-tests:animation-demos --> :compose:foundation:foundation-layout
  :compose:animation:animation:integration-tests:animation-demos --> :compose:integration-tests:demos:common
  :compose:animation:animation:integration-tests:animation-demos --> :compose:runtime:runtime
  :compose:animation:animation:integration-tests:animation-demos --> :compose:ui:ui
  :compose:animation:animation:integration-tests:animation-demos --> :compose:ui:ui-text
  :compose:animation:animation:integration-tests:animation-demos --> :compose:animation:animation
  :compose:animation:animation:integration-tests:animation-demos --> :compose:animation:animation-graphics
  :compose:animation:animation:integration-tests:animation-demos --> :compose:ui:ui:ui-samples
  :compose:animation:animation:integration-tests:animation-demos --> :compose:animation:animation:animation-samples
  :compose:animation:animation:integration-tests:animation-demos --> :compose:animation:animation-core:animation-core-samples
  :compose:animation:animation:integration-tests:animation-demos --> :compose:foundation:foundation
  :compose:animation:animation:integration-tests:animation-demos --> :compose:material:material
  :compose:animation:animation:integration-tests:animation-demos --> :compose:ui:ui-tooling-preview
  :compose:animation:animation:integration-tests:animation-demos --> :compose:lint:internal-lint-checks
  :compose:animation:animation:integration-tests:animation-demos --> :lint-checks
  :compose:runtime:runtime-saveable --> :compose:ui:ui
  :compose:runtime:runtime-saveable --> :compose:ui:ui-test-junit4
  :compose:runtime:runtime-saveable --> :compose:test-utils
  :compose:runtime:runtime-saveable --> :compose:runtime:runtime
  :compose:runtime:runtime-saveable --> :collection:collection
  :compose:runtime:runtime-saveable --> :compose:lint:internal-lint-checks
  :compose:runtime:runtime-saveable --> :lint-checks
  :compose:runtime:runtime-saveable --> :compose:runtime:runtime-saveable:runtime-saveable-samples
  :core:core-uri --> :lint-checks
  :annotation:annotation-sampled --> :lint-checks
  :compose:ui:ui --> :compose:animation:animation-core
  :compose:ui:ui --> :compose:foundation:foundation
  :compose:ui:ui --> :compose:foundation:foundation-layout
  :compose:ui:ui --> :compose:material:material
  :compose:ui:ui --> :compose:test-utils
  :compose:ui:ui --> :internal-testutils-fonts
  :compose:ui:ui --> :compose:ui:ui-test-junit4
  :compose:ui:ui --> :internal-testutils-runtime
  :compose:ui:ui --> :compose:runtime:runtime-saveable
  :compose:ui:ui --> :compose:ui:ui-geometry
  :compose:ui:ui --> :compose:ui:ui-graphics
  :compose:ui:ui --> :compose:ui:ui-text
  :compose:ui:ui --> :compose:ui:ui-unit
  :compose:ui:ui --> :compose:ui:ui-util
  :compose:ui:ui --> :collection:collection
  :compose:ui:ui --> :compose:runtime:runtime
  :compose:ui:ui --> :annotation:annotation
  :compose:ui:ui --> :lifecycle:lifecycle-common
  :compose:ui:ui --> :lifecycle:lifecycle-runtime
  :compose:ui:ui --> :lifecycle:lifecycle-runtime-compose
  :compose:ui:ui --> :lifecycle:lifecycle-viewmodel
  :compose:ui:ui --> :compose:ui:ui-android-stubs
  :compose:ui:ui --> :compose:lint:internal-lint-checks
  :compose:ui:ui --> :lint-checks
  :compose:ui:ui --> :compose:ui:ui:ui-samples
  :compose:ui:ui --> :compose:ui:ui-uikit
  :lint-checks:integration-tests --> :lint-checks
  :internal-testutils-espresso --> :lint-checks
  :compose:integration-tests:demos:common --> :compose:runtime:runtime
  :compose:integration-tests:demos:common --> :compose:lint:internal-lint-checks
  :compose:integration-tests:demos:common --> :lint-checks
  :compose:runtime:runtime-rxjava3:runtime-rxjava3-samples --> :annotation:annotation-sampled
  :compose:runtime:runtime-rxjava3:runtime-rxjava3-samples --> :compose:runtime:runtime-rxjava3
  :compose:runtime:runtime-rxjava3:runtime-rxjava3-samples --> :compose:lint:internal-lint-checks
  :compose:runtime:runtime-rxjava3:runtime-rxjava3-samples --> :lint-checks
  :compose:runtime:runtime-rxjava2:runtime-rxjava2-samples --> :annotation:annotation-sampled
  :compose:runtime:runtime-rxjava2:runtime-rxjava2-samples --> :compose:runtime:runtime-rxjava2
  :compose:runtime:runtime-rxjava2:runtime-rxjava2-samples --> :compose:lint:internal-lint-checks
  :compose:runtime:runtime-rxjava2:runtime-rxjava2-samples --> :lint-checks
  :internal-testutils-gradle-plugin --> :lint-checks
  :compose:runtime:runtime --> :annotation:annotation
  :compose:runtime:runtime --> :collection:collection
  :compose:runtime:runtime --> :compose:runtime:runtime-test-utils
  :compose:runtime:runtime --> :compose:lint:internal-lint-checks
  :compose:runtime:runtime --> :lint-checks
  :compose:runtime:runtime --> :compose:runtime:runtime:runtime-samples
  :compose:material3:material3-common --> :compose:material3:material3
  :compose:material3:material3-common --> :compose:test-utils
  :compose:material3:material3-common --> :compose:foundation:foundation
  :compose:material3:material3-common --> :compose:foundation:foundation-layout
  :compose:material3:material3-common --> :compose:runtime:runtime
  :compose:material3:material3-common --> :compose:ui:ui-graphics
  :compose:material3:material3-common --> :compose:ui:ui-text
  :compose:material3:material3-common --> :compose:ui:ui-util
  :compose:material3:material3-common --> :compose:lint:internal-lint-checks
  :compose:material3:material3-common --> :lint-checks
  :compose:material3:material3-window-size-class --> :compose:test-utils
  :compose:material3:material3-window-size-class --> :compose:foundation:foundation
  :compose:material3:material3-window-size-class --> :compose:runtime:runtime
  :compose:material3:material3-window-size-class --> :compose:ui:ui
  :compose:material3:material3-window-size-class --> :compose:ui:ui-unit
  :compose:material3:material3-window-size-class --> :compose:ui:ui-util
  :compose:material3:material3-window-size-class --> :compose:lint:internal-lint-checks
  :compose:material3:material3-window-size-class --> :lint-checks
  :compose:ui:ui-test:ui-test-samples --> :annotation:annotation-sampled
  :compose:ui:ui-test:ui-test-samples --> :compose:ui:ui-test
  :compose:ui:ui-test:ui-test-samples --> :compose:ui:ui-test-junit4
  :compose:ui:ui-test:ui-test-samples --> :compose:lint:internal-lint-checks
  :compose:ui:ui-test:ui-test-samples --> :lint-checks
  :internal-testutils-navigation --> :annotation:annotation
  :internal-testutils-navigation --> :navigation:navigation-common
  :internal-testutils-navigation --> :navigation:navigation-testing
  :internal-testutils-navigation --> :kruth:kruth
  :internal-testutils-navigation --> :lint-checks
  :compose:runtime:runtime-tracing --> :compose:runtime:runtime
  :compose:runtime:runtime-tracing --> :lint-checks
  :savedstate:savedstate --> :annotation:annotation
  :savedstate:savedstate --> :core:core-bundle
  :savedstate:savedstate --> :lifecycle:lifecycle-common
  :savedstate:savedstate --> :lifecycle:lifecycle-runtime
  :savedstate:savedstate --> :lint-checks
  :compose:ui:ui-android-stubs --> :lint-checks
  :compose:material3:material3 --> :compose:material3:material3:material3-samples
  :compose:material3:material3 --> :compose:test-utils
  :compose:material3:material3 --> :compose:foundation:foundation-layout
  :compose:material3:material3 --> :compose:foundation:foundation
  :compose:material3:material3 --> :compose:material:material-ripple
  :compose:material3:material3 --> :compose:runtime:runtime
  :compose:material3:material3 --> :compose:ui:ui-graphics
  :compose:material3:material3 --> :compose:ui:ui-text
  :compose:material3:material3 --> :graphics:graphics-shapes
  :compose:material3:material3 --> :compose:animation:animation-core
  :compose:material3:material3 --> :compose:ui:ui-util
  :compose:material3:material3 --> :annotation:annotation
  :compose:material3:material3 --> :collection:collection
  :compose:material3:material3 --> :compose:ui:ui-test-junit4
  :compose:material3:material3 --> :compose:lint:internal-lint-checks
  :compose:material3:material3 --> :lint-checks
  :compose:material3:material3 --> :compose:ui:ui-test
  :compose:ui:ui-test-manifest:integration-tests:testapp --> :compose:ui:ui-test-junit4
  :compose:ui:ui-test-manifest:integration-tests:testapp --> :compose:ui:ui-test-manifest
  :compose:ui:ui-test-manifest:integration-tests:testapp --> :compose:ui:ui
  :compose:ui:ui-test-manifest:integration-tests:testapp --> :compose:lint:internal-lint-checks
  :compose:ui:ui-test-manifest:integration-tests:testapp --> :lint-checks
  :compose:runtime:runtime-rxjava2 --> :compose:ui:ui-test-junit4
  :compose:runtime:runtime-rxjava2 --> :compose:test-utils
  :compose:runtime:runtime-rxjava2 --> :compose:runtime:runtime
  :compose:runtime:runtime-rxjava2 --> :compose:lint:internal-lint-checks
  :compose:runtime:runtime-rxjava2 --> :lint-checks
  :compose:runtime:runtime-rxjava2 --> :compose:runtime:runtime-rxjava2:runtime-rxjava2-samples
  :compose:lint:common-test --> :lint-checks
  :compose:desktop:desktop:desktop-samples-material3 --> :compose:material3:material3
  :compose:desktop:desktop:desktop-samples-material3 --> :compose:desktop:desktop
  :compose:desktop:desktop:desktop-samples-material3 --> :lint-checks
  :navigation:navigation-compose --> :compose:material:material
  :navigation:navigation-compose --> :compose:test-utils
  :navigation:navigation-compose --> :compose:ui:ui-tooling
  :navigation:navigation-compose --> :navigation:navigation-testing
  :navigation:navigation-compose --> :internal-testutils-navigation
  :navigation:navigation-compose --> :compose:ui:ui-test-junit4
  :navigation:navigation-compose --> :lifecycle:lifecycle-common
  :navigation:navigation-compose --> :lifecycle:lifecycle-viewmodel
  :navigation:navigation-compose --> :lifecycle:lifecycle-viewmodel-savedstate
  :navigation:navigation-compose --> :core:core-bundle
  :navigation:navigation-compose --> :compose:animation:animation
  :navigation:navigation-compose --> :compose:runtime:runtime
  :navigation:navigation-compose --> :compose:runtime:runtime-saveable
  :navigation:navigation-compose --> :compose:ui:ui
  :navigation:navigation-compose --> :lifecycle:lifecycle-runtime
  :navigation:navigation-compose --> :lifecycle:lifecycle-runtime-compose
  :navigation:navigation-compose --> :lifecycle:lifecycle-viewmodel-compose
  :navigation:navigation-compose --> :navigation:navigation-common
  :navigation:navigation-compose --> :navigation:navigation-runtime
  :navigation:navigation-compose --> :savedstate:savedstate
  :navigation:navigation-compose --> :compose:foundation:foundation-layout
  :navigation:navigation-compose --> :compose:animation:animation-core
  :navigation:navigation-compose --> :kruth:kruth
  :navigation:navigation-compose --> :compose:ui:ui-test
  :navigation:navigation-compose --> :compose:lint:internal-lint-checks
  :navigation:navigation-compose --> :lint-checks
  :lifecycle:lifecycle-runtime-testing --> :lifecycle:lifecycle-runtime
  :lifecycle:lifecycle-runtime-testing --> :kruth:kruth
  :lifecycle:lifecycle-runtime-testing --> :lint-checks
  :lifecycle:lifecycle-runtime-testing --> :lifecycle:lifecycle-runtime-testing-lint
  :compose:ui:ui-geometry --> :compose:runtime:runtime
  :compose:ui:ui-geometry --> :compose:ui:ui-util
  :compose:ui:ui-geometry --> :compose:lint:internal-lint-checks
  :compose:ui:ui-geometry --> :lint-checks
  :compose:mpp:demo --> :compose:foundation:foundation
  :compose:mpp:demo --> :compose:foundation:foundation-layout
  :compose:mpp:demo --> :compose:material3:material3
  :compose:mpp:demo --> :compose:material3:material3-window-size-class
  :compose:mpp:demo --> :compose:material3:adaptive:adaptive
  :compose:mpp:demo --> :compose:material3:adaptive:adaptive-layout
  :compose:mpp:demo --> :compose:material3:adaptive:adaptive-navigation
  :compose:mpp:demo --> :compose:material:material
  :compose:mpp:demo --> :compose:mpp
  :compose:mpp:demo --> :compose:runtime:runtime
  :compose:mpp:demo --> :compose:ui:ui
  :compose:mpp:demo --> :compose:ui:ui-graphics
  :compose:mpp:demo --> :compose:ui:ui-text
  :compose:mpp:demo --> :lifecycle:lifecycle-common
  :compose:mpp:demo --> :lifecycle:lifecycle-runtime
  :compose:mpp:demo --> :lifecycle:lifecycle-runtime-compose
  :compose:mpp:demo --> :navigation:navigation-common
  :compose:mpp:demo --> :navigation:navigation-compose
  :compose:mpp:demo --> :navigation:navigation-runtime
  :compose:mpp:demo --> :compose:desktop:desktop
  :navigation:navigation-runtime --> :annotation:annotation
  :navigation:navigation-runtime --> :core:core-bundle
  :navigation:navigation-runtime --> :core:core-uri
  :navigation:navigation-runtime --> :lifecycle:lifecycle-common
  :navigation:navigation-runtime --> :lifecycle:lifecycle-runtime
  :navigation:navigation-runtime --> :lifecycle:lifecycle-viewmodel
  :navigation:navigation-runtime --> :lifecycle:lifecycle-viewmodel-savedstate
  :navigation:navigation-runtime --> :navigation:navigation-common
  :navigation:navigation-runtime --> :savedstate:savedstate
  :navigation:navigation-runtime --> :collection:collection
  :navigation:navigation-runtime --> :internal-testutils-runtime
  :navigation:navigation-runtime --> :kruth:kruth
  :navigation:navigation-runtime --> :navigation:navigation-testing
  :navigation:navigation-runtime --> :internal-testutils-navigation
  :navigation:navigation-runtime --> :lint-checks
  :compose:animation:animation-tooling-internal --> :lint-checks
  :compose:ui:ui-viewbinding:ui-viewbinding-samples --> :compose:foundation:foundation
  :compose:ui:ui-viewbinding:ui-viewbinding-samples --> :compose:test-utils
  :compose:ui:ui-viewbinding:ui-viewbinding-samples --> :internal-testutils-runtime
  :compose:ui:ui-viewbinding:ui-viewbinding-samples --> :annotation:annotation-sampled
  :compose:ui:ui-viewbinding:ui-viewbinding-samples --> :compose:ui:ui
  :compose:ui:ui-viewbinding:ui-viewbinding-samples --> :compose:ui:ui-viewbinding
  :compose:ui:ui-viewbinding:ui-viewbinding-samples --> :compose:lint:internal-lint-checks
  :compose:ui:ui-viewbinding:ui-viewbinding-samples --> :lint-checks
  :compose:foundation:foundation:integration-tests:foundation-demos --> :compose:ui:ui-tooling
  :compose:foundation:foundation:integration-tests:foundation-demos --> :compose:animation:animation
  :compose:foundation:foundation:integration-tests:foundation-demos --> :compose:foundation:foundation
  :compose:foundation:foundation:integration-tests:foundation-demos --> :compose:foundation:foundation-layout
  :compose:foundation:foundation:integration-tests:foundation-demos --> :compose:foundation:foundation:foundation-samples
  :compose:foundation:foundation:integration-tests:foundation-demos --> :compose:integration-tests:demos:common
  :compose:foundation:foundation:integration-tests:foundation-demos --> :compose:material:material
  :compose:foundation:foundation:integration-tests:foundation-demos --> :compose:runtime:runtime
  :compose:foundation:foundation:integration-tests:foundation-demos --> :compose:ui:ui
  :compose:foundation:foundation:integration-tests:foundation-demos --> :compose:ui:ui-util
  :compose:foundation:foundation:integration-tests:foundation-demos --> :compose:ui:ui-text
  :compose:foundation:foundation:integration-tests:foundation-demos --> :compose:ui:ui-text:ui-text-samples
  :compose:foundation:foundation:integration-tests:foundation-demos --> :compose:ui:ui-tooling-preview
  :compose:foundation:foundation:integration-tests:foundation-demos --> :internal-testutils-fonts
  :compose:foundation:foundation:integration-tests:foundation-demos --> :compose:lint:internal-lint-checks
  :compose:foundation:foundation:integration-tests:foundation-demos --> :lint-checks
  :compose:runtime:runtime-saveable:runtime-saveable-samples --> :annotation:annotation-sampled
  :compose:runtime:runtime-saveable:runtime-saveable-samples --> :compose:runtime:runtime
  :compose:runtime:runtime-saveable:runtime-saveable-samples --> :compose:runtime:runtime-saveable
  :compose:runtime:runtime-saveable:runtime-saveable-samples --> :compose:lint:internal-lint-checks
  :compose:runtime:runtime-saveable:runtime-saveable-samples --> :lint-checks
  :lifecycle:lifecycle-viewmodel --> :annotation:annotation
  :lifecycle:lifecycle-viewmodel --> :kruth:kruth
  :lifecycle:lifecycle-viewmodel --> :lint-checks
  :compose:foundation:foundation --> :compose:test-utils
  :compose:foundation:foundation --> :internal-testutils-fonts
  :compose:foundation:foundation --> :internal-testutils-runtime
  :compose:foundation:foundation --> :compose:animation:animation
  :compose:foundation:foundation --> :compose:runtime:runtime
  :compose:foundation:foundation --> :compose:ui:ui
  :compose:foundation:foundation --> :collection:collection
  :compose:foundation:foundation --> :compose:ui:ui-text
  :compose:foundation:foundation --> :compose:ui:ui-util
  :compose:foundation:foundation --> :compose:foundation:foundation-layout
  :compose:foundation:foundation --> :annotation:annotation
  :compose:foundation:foundation --> :compose:ui:ui-test-junit4
  :compose:foundation:foundation --> :compose:lint:internal-lint-checks
  :compose:foundation:foundation --> :lint-checks
  :compose:foundation:foundation --> :compose:foundation:foundation:foundation-samples
  :compose:material3:adaptive:adaptive-navigation --> :compose:material3:adaptive:adaptive-layout
  :compose:material3:adaptive:adaptive-navigation --> :compose:foundation:foundation
  :compose:material3:adaptive:adaptive-navigation --> :compose:ui:ui-util
  :compose:material3:adaptive:adaptive-navigation --> :compose:material3:material3
  :compose:material3:adaptive:adaptive-navigation --> :compose:test-utils
  :compose:material3:adaptive:adaptive-navigation --> :compose:lint:internal-lint-checks
  :compose:material3:adaptive:adaptive-navigation --> :lint-checks
  :compose:runtime:runtime:runtime-samples --> :annotation:annotation-sampled
  :compose:runtime:runtime:runtime-samples --> :compose:runtime:runtime
  :compose:runtime:runtime:runtime-samples --> :compose:lint:internal-lint-checks
  :compose:runtime:runtime:runtime-samples --> :lint-checks
  :compose:ui:ui-test-manifest --> :compose:lint:internal-lint-checks
  :compose:ui:ui-test-manifest --> :lint-checks
  :compose:ui:ui-test-manifest --> :compose:ui:ui-test-manifest-lint
  :compose:material:material-ripple --> :compose:test-utils
  :compose:material:material-ripple --> :compose:foundation:foundation
  :compose:material:material-ripple --> :compose:runtime:runtime
  :compose:material:material-ripple --> :collection:collection
  :compose:material:material-ripple --> :compose:animation:animation
  :compose:material:material-ripple --> :compose:ui:ui-util
  :compose:material:material-ripple --> :compose:lint:internal-lint-checks
  :compose:material:material-ripple --> :lint-checks
  :compose:lint:common --> :lint-checks
  :compose:material3:adaptive:adaptive --> :annotation:annotation
  :compose:material3:adaptive:adaptive --> :compose:foundation:foundation
  :compose:material3:adaptive:adaptive --> :compose:ui:ui-geometry
  :compose:material3:adaptive:adaptive --> :window:window-core
  :compose:material3:adaptive:adaptive --> :compose:material3:material3
  :compose:material3:adaptive:adaptive --> :compose:test-utils
  :compose:material3:adaptive:adaptive --> :compose:lint:internal-lint-checks
  :compose:material3:adaptive:adaptive --> :lint-checks
  :internal-testutils-truth --> :lint-checks
  :compose:ui:ui-text-google-fonts --> :compose:ui:ui-test-junit4
  :compose:ui:ui-text-google-fonts --> :compose:ui:ui-text
  :compose:ui:ui-text-google-fonts --> :compose:lint:internal-lint-checks
  :compose:ui:ui-text-google-fonts --> :lint-checks
  :compose:material3:adaptive:adaptive-layout --> :compose:material3:adaptive:adaptive
  :compose:material3:adaptive:adaptive-layout --> :compose:animation:animation-core
  :compose:material3:adaptive:adaptive-layout --> :compose:ui:ui
  :compose:material3:adaptive:adaptive-layout --> :compose:animation:animation
  :compose:material3:adaptive:adaptive-layout --> :compose:foundation:foundation
  :compose:material3:adaptive:adaptive-layout --> :compose:foundation:foundation-layout
  :compose:material3:adaptive:adaptive-layout --> :compose:ui:ui-geometry
  :compose:material3:adaptive:adaptive-layout --> :compose:ui:ui-util
  :compose:material3:adaptive:adaptive-layout --> :window:window-core
  :compose:material3:adaptive:adaptive-layout --> :collection:collection
  :compose:material3:adaptive:adaptive-layout --> :compose:material3:material3
  :compose:material3:adaptive:adaptive-layout --> :compose:test-utils
  :compose:material3:adaptive:adaptive-layout --> :annotation:annotation
  :compose:material3:adaptive:adaptive-layout --> :kruth:kruth
  :compose:material3:adaptive:adaptive-layout --> :compose:lint:internal-lint-checks
  :compose:material3:adaptive:adaptive-layout --> :lint-checks
  :compose:ui:ui-graphics:ui-graphics-samples --> :compose:ui:ui-unit
  :compose:ui:ui-graphics:ui-graphics-samples --> :annotation:annotation-sampled
  :compose:ui:ui-graphics:ui-graphics-samples --> :compose:ui:ui-graphics
  :compose:ui:ui-graphics:ui-graphics-samples --> :compose:ui:ui-util
  :compose:ui:ui-graphics:ui-graphics-samples --> :compose:lint:internal-lint-checks
  :compose:ui:ui-graphics:ui-graphics-samples --> :lint-checks
  :annotation:annotation --> :lint-checks
  :lifecycle:lifecycle-runtime-lint --> :lint-checks
  :lifecycle:lifecycle-viewmodel-compose --> :lifecycle:lifecycle-runtime
  :lifecycle:lifecycle-viewmodel-compose --> :compose:test-utils
  :lifecycle:lifecycle-viewmodel-compose --> :lifecycle:lifecycle-common
  :lifecycle:lifecycle-viewmodel-compose --> :lifecycle:lifecycle-viewmodel
  :lifecycle:lifecycle-viewmodel-compose --> :lifecycle:lifecycle-viewmodel-savedstate
  :lifecycle:lifecycle-viewmodel-compose --> :savedstate:savedstate
  :lifecycle:lifecycle-viewmodel-compose --> :compose:runtime:runtime
  :lifecycle:lifecycle-viewmodel-compose --> :compose:runtime:runtime-saveable
  :lifecycle:lifecycle-viewmodel-compose --> :compose:ui:ui
  :lifecycle:lifecycle-viewmodel-compose --> :core:core-bundle
  :lifecycle:lifecycle-viewmodel-compose --> :compose:lint:internal-lint-checks
  :lifecycle:lifecycle-viewmodel-compose --> :lint-checks
  :compose:runtime:runtime-livedata --> :compose:ui:ui-test-junit4
  :compose:runtime:runtime-livedata --> :compose:test-utils
  :compose:runtime:runtime-livedata --> :compose:runtime:runtime
  :compose:runtime:runtime-livedata --> :compose:lint:internal-lint-checks
  :compose:runtime:runtime-livedata --> :lint-checks
  :compose:runtime:runtime-livedata --> :compose:runtime:runtime-livedata:runtime-livedata-samples
  :compose:material:material:material-samples --> :annotation:annotation-sampled
  :compose:material:material:material-samples --> :compose:material:material
  :compose:material:material:material-samples --> :compose:lint:internal-lint-checks
  :compose:material:material:material-samples --> :lint-checks
  :compose:runtime:runtime-rxjava3 --> :compose:ui:ui-test-junit4
  :compose:runtime:runtime-rxjava3 --> :compose:test-utils
  :compose:runtime:runtime-rxjava3 --> :compose:runtime:runtime
  :compose:runtime:runtime-rxjava3 --> :compose:lint:internal-lint-checks
  :compose:runtime:runtime-rxjava3 --> :lint-checks
  :compose:runtime:runtime-rxjava3 --> :compose:runtime:runtime-rxjava3:runtime-rxjava3-samples
  :lifecycle:lifecycle-common --> :annotation:annotation
  :lifecycle:lifecycle-common --> :lint-checks
  :compose:foundation:foundation-layout:foundation-layout-samples --> :annotation:annotation-sampled
  :compose:foundation:foundation-layout:foundation-layout-samples --> :compose:foundation:foundation
  :compose:foundation:foundation-layout:foundation-layout-samples --> :compose:foundation:foundation-layout
  :compose:foundation:foundation-layout:foundation-layout-samples --> :compose:lint:internal-lint-checks
  :compose:foundation:foundation-layout:foundation-layout-samples --> :lint-checks
  :compose:desktop:desktop --> :compose:foundation:foundation
  :compose:desktop:desktop --> :compose:material:material
  :compose:desktop:desktop --> :compose:runtime:runtime
  :compose:desktop:desktop --> :compose:ui:ui
  :compose:desktop:desktop --> :compose:ui:ui-tooling-preview
  :compose:desktop:desktop --> :compose:ui:ui-util
  :compose:desktop:desktop --> :compose:ui:ui-test-junit4
  :compose:desktop:desktop --> :lint-checks
  :internal-testutils-runtime --> :lint-checks
  :internal-testutils-lifecycle --> :lifecycle:lifecycle-runtime
  :internal-testutils-lifecycle --> :annotation:annotation
  :internal-testutils-lifecycle --> :lint-checks
  :compose:test-utils --> :compose:material:material
  :compose:test-utils --> :compose:ui:ui-test-junit4
  :compose:test-utils --> :compose:ui:ui-android-stubs
  :compose:test-utils --> :compose:runtime:runtime
  :compose:test-utils --> :compose:ui:ui-unit
  :compose:test-utils --> :compose:ui:ui-graphics
  :compose:test-utils --> :compose:lint:internal-lint-checks
  :compose:test-utils --> :lint-checks
  :compose:animation:animation --> :compose:foundation:foundation
  :compose:animation:animation --> :compose:ui:ui-test-junit4
  :compose:animation:animation --> :compose:test-utils
  :compose:animation:animation --> :compose:animation:animation-core
  :compose:animation:animation --> :compose:foundation:foundation-layout
  :compose:animation:animation --> :compose:runtime:runtime
  :compose:animation:animation --> :compose:ui:ui
  :compose:animation:animation --> :compose:ui:ui-geometry
  :compose:animation:animation --> :collection:collection
  :compose:animation:animation --> :compose:ui:ui-util
  :compose:animation:animation --> :compose:lint:internal-lint-checks
  :compose:animation:animation --> :lint-checks
  :compose:animation:animation --> :compose:animation:animation:animation-samples
  :graphics:graphics-shapes --> :collection:collection
  :graphics:graphics-shapes --> :annotation:annotation
  :graphics:graphics-shapes --> :lint-checks
  :compose:animation:animation-lint --> :compose:lint:common
  :compose:animation:animation-lint --> :lint-checks
  :compose:animation:animation-lint --> :compose:lint:common-test
  :lifecycle:lifecycle-runtime --> :lifecycle:lifecycle-common
  :lifecycle:lifecycle-runtime --> :annotation:annotation
  :lifecycle:lifecycle-runtime --> :internal-testutils-lifecycle
  :lifecycle:lifecycle-runtime --> :kruth:kruth
  :lifecycle:lifecycle-runtime --> :lint-checks
  :lifecycle:lifecycle-runtime --> :lifecycle:lifecycle-runtime-lint
  :compose:ui:ui-graphics --> :compose:ui:ui-graphics:ui-graphics-samples
  :compose:ui:ui-graphics --> :compose:ui:ui-test-junit4
  :compose:ui:ui-graphics --> :compose:test-utils
  :compose:ui:ui-graphics --> :compose:ui:ui-unit
  :compose:ui:ui-graphics --> :compose:runtime:runtime
  :compose:ui:ui-graphics --> :compose:ui:ui-util
  :compose:ui:ui-graphics --> :compose:ui:ui-geometry
  :compose:ui:ui-graphics --> :collection:collection
  :compose:ui:ui-graphics --> :annotation:annotation
  :compose:ui:ui-graphics --> :compose:lint:internal-lint-checks
  :compose:ui:ui-graphics --> :lint-checks
  :collection:collection --> :annotation:annotation
  :collection:collection --> :internal-testutils-truth
  :collection:collection --> :lint-checks
  :core:core-bundle --> :lint-checks
  :internal-testutils-fonts --> :compose:lint:internal-lint-checks
  :internal-testutils-fonts --> :lint-checks
  :compose:ui:ui-text:ui-text-samples --> :annotation:annotation-sampled
  :compose:ui:ui-text:ui-text-samples --> :compose:ui:ui
  :compose:ui:ui-text:ui-text-samples --> :compose:ui:ui-text
  :compose:ui:ui-text:ui-text-samples --> :compose:lint:internal-lint-checks
  :compose:ui:ui-text:ui-text-samples --> :lint-checks
  :compose:animation:animation-graphics --> :compose:foundation:foundation
  :compose:animation:animation-graphics --> :compose:ui:ui-test-junit4
  :compose:animation:animation-graphics --> :compose:test-utils
  :compose:animation:animation-graphics --> :compose:animation:animation
  :compose:animation:animation-graphics --> :compose:foundation:foundation-layout
  :compose:animation:animation-graphics --> :compose:runtime:runtime
  :compose:animation:animation-graphics --> :compose:ui:ui
  :compose:animation:animation-graphics --> :compose:ui:ui-geometry
  :compose:animation:animation-graphics --> :collection:collection
  :compose:animation:animation-graphics --> :compose:ui:ui-util
  :compose:animation:animation-graphics --> :compose:lint:internal-lint-checks
  :compose:animation:animation-graphics --> :lint-checks
  :compose:animation:animation-graphics --> :compose:animation:animation-graphics:animation-graphics-samples
```