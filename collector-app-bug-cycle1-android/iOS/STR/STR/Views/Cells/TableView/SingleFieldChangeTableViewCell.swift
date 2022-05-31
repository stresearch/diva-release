//
//  SingleFieldChangeTableViewCell.swift
//  STR
//
//  Created by Srujan on 03/02/20.
//  
//

import UIKit

protocol SingleFieldChangeTableViewCellDelegate: class {
    func didTapOnChangeButton(for row: TableRow)
}

class SingleFieldChangeTableViewCell: UITableViewCell {

    @IBOutlet weak var textField: TextField!
    
    var currentRow: TableRow! {
        didSet {
            if let row = currentRow { // TODO: need to clean up here
                textField.placeholder = "Enter " + row.title.lowercased()
            }
        }
    }
    
    weak var delegate: SingleFieldTableViewCellDelegate?
    weak var changeFieldDelegate: SingleFieldChangeTableViewCellDelegate?
  
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
        textField.addTarget(self, action: #selector(didChangeEditing(_:)), for: .valueChanged)
    }

    @objc private func didChangeEditing(_ sender: UITextField) {
        if let value = sender.text {
            delegate?.didChangeValue(for: currentRow, value: value)
        }
    }
  
    //MARK: IBActions
    @IBAction func changeBtnAction(_ sender: UIButton) {
        changeFieldDelegate?.didTapOnChangeButton(for: currentRow)
    }
}
