//
//  ActivityNameTableViewCell.swift
//  STR
//
//  Created by Srujan on 02/01/20.
//  
//

import UIKit

class ActivityNameTableViewCell: UITableViewCell {
    
    // MARK:- Outlets
    @IBOutlet weak var nameLbl: UILabel!
    @IBOutlet weak var bgBiew: UIView!
    
    override func setHighlighted(_ highlighted: Bool, animated: Bool) {
        nameLbl.textColor = highlighted ? .white : .darkText
        bgBiew.backgroundColor = highlighted ? Color.NamedColor.main.color : .white
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        nameLbl.textColor = selected ? .white : .darkText
        bgBiew.backgroundColor = selected ? Color.NamedColor.main.color : .white
    }
}
