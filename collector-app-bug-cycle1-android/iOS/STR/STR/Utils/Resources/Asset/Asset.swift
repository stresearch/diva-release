//
//  Asset.swift
//  STR
//
//  Created by Srujan on 10/06/19.
//  
//

import UIKit

enum Asset: String {

    // MARK: - Home
    case panic = ""
    case back = "back"
    case car = "ic_car"
    case motorCycle = "ic_motorcycle"
    case project = "ic_project"
    
}

extension UIImage { convenience init?(asset: Asset) { self.init(named: asset.rawValue) } }
