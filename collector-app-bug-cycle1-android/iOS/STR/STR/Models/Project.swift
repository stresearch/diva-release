// This file was generated from JSON Schema using quicktype, do not modify it directly.
// To parse the JSON, add this file to your project and do:
//
//   let project = try? newJSONDecoder().decode(Project.self, from: jsonData)

import Foundation

typealias ListStrCollectionsAssignmentItem = ListStrCollectionsAssignmentsQuery.Data.ListStrCollectionsAssignment.Item
typealias ListStrCollectionsAssignment = [ListStrCollectionsAssignmentsQuery.Data.ListStrCollectionsAssignment.Item]

// MARK: - Project
class Project {
    
    let title: String
    var collections: ListStrCollectionsAssignment = []
    
    init(title: String, collections: ListStrCollectionsAssignment) {
        self.title = title
        self.collections = collections
    }
}
