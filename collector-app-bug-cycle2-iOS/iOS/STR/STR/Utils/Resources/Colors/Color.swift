//
//  Colors.swift
//  STR
//
//  Created by Srujan on 10/06/19.
//  
//

import UIKit

enum Color: String {

    case main = "#000000"
    
    var color: UIColor { return hexStringToUIColor(hex: self.rawValue) }

    enum NamedColor: String {
        case main = "Main Color"
        var color: UIColor { return UIColor(named: self.rawValue) ?? .red}
    }
    
    func hexStringToUIColor(hex: String) -> UIColor {
        var cString: String = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

        if cString.hasPrefix("#") { cString.remove(at: cString.startIndex) }

        if (cString.count) != 6 { return UIColor.gray }
        
        var rgbValue: UInt32 = 0
        Scanner(string: cString).scanHexInt32(&rgbValue)

        return UIColor(
            red: CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0,
            green: CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0,
            blue: CGFloat(rgbValue & 0x0000FF) / 255.0,
            alpha: CGFloat(1.0)
        )
    }
}
