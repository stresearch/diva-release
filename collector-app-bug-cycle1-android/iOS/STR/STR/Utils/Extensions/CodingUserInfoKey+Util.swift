//
//  CodingUserInfoKey+Util.swift
//  CoreDataCodable
//
//  Created by Andrea Prearo on 3/29/18.
//  Copyright © 2018 Andrea Prearo. All rights reserved.
//

import Foundation

extension CodingUserInfoKey {
    // Helper property to retrieve the Core Data managed object context
    public static let managedObjectContext = CodingUserInfoKey(rawValue: "managedObjectContext")
}
