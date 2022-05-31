//
//  ProjectService.swift
//  STR
//
//  Created by Srujan on 08/01/20.
//  
//

import Foundation

class ProjectService {
    
    var currentProject: Project?
    var currentCollection: ListStrCollectionsAssignmentItem?
    var trainingVideoUrl = ""
    var trainingVideoTextIndex = 0
    static let instance = ProjectService()
    private init() {}
    
    var isPracticeProject: Bool?
    
    func clear() {
        currentProject = nil
        currentCollection = nil
        isPracticeProject = nil
    }
}
