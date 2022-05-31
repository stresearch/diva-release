//
//  SingleDropDownTableViewCell.swift
//  STR
//
//  Created by GovindPrasad on 8/5/20.
//  
//

import UIKit

class SingleDropDownTableViewCell: UITableViewCell {
    
    @IBOutlet weak var textField: customDropDown!
    @IBOutlet weak var placeholderLbl: PaddingLabel!
    
    var currentRow: TableRow! {
        didSet {
            //            if let row = currentRow { // TODO: need to clean up here
            //                                textField.placeholder = "Enter " + row.title.lowercased()
            //            }
        }
    }
    
    override func awakeFromNib() {
        super.awakeFromNib()
    }
    
}
