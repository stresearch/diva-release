//
//  UserDefautls+Extension.swift
//  STR
//
//  Created by Srujan on 21/04/20.
//  
//

import Foundation

extension UserDefaults {

    enum Keys: String, CaseIterable {

        case subjectEmail
        case currentProject
        case bucketNameForCheck
        case deleteRatingVideo
    }
    
    enum Key: String, CaseIterable {

        case login
    }

    func reset() {
        Keys.allCases.forEach { removeObject(forKey: $0.rawValue) }
    }

}
