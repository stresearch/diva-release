//
//  ErrorPresentable.swift
//  STR
//
//  Created by Srujan on 10/06/19.
//  
//

import Foundation

protocol ErrorPresentable: Error {
    var title: String? { get }
    var message: String? { get }
}
