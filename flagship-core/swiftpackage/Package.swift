// swift-tools-version:5.3
import PackageDescription

let package = Package(
    name: "Flagship",
    platforms: [
        .iOS(.v14)
    ],
    products: [
        .library(
            name: "Flagship",
            targets: ["Flagship"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "Flagship",
            path: "./Flagship.xcframework"
        ),
    ]
)
