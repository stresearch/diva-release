//
//  SingleFieldTableViewCell.swift
//  STR
//
//  Created by Srujan on 31/12/19.
//  
//

import UIKit

protocol SingleFieldTableViewCellDelegate: class{
    func didChangeValue(for row: TableRow, value: String)
}

class SingleFieldTableViewCell: UITableViewCell {

    @IBOutlet weak var textField: TextField!
    
    var currentRow: TableRow! {
        didSet {
            if let row = currentRow { // TODO: need to clean up here 
                textField.placeholder = "Enter " + row.title.lowercased()
            }
        }
    }
    
    weak var delegate: SingleFieldTableViewCellDelegate?
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
        textField.addTarget(self, action: #selector(didChangeEditing(_:)), for: .editingChanged)
    }

    @objc private func didChangeEditing(_ sender: UITextField) {
        if let value = sender.text {
            delegate?.didChangeValue(for: currentRow, value: value)
        }
    }

}
