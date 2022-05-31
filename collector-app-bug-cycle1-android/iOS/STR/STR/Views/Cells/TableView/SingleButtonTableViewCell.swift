//
//  SingleButtonTableViewCell.swift
//  STR
//
//  Created by Srujan on 02/04/20.
//  
//

import UIKit

protocol SingleButtonCellDelegate: class {
    func didTapOnButton()
}

class SingleButtonTableViewCell: UITableViewCell {

    weak var delegate: SingleButtonCellDelegate?
    
    @IBOutlet weak var titleBtn: UIButton!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    //Actions
    @IBAction func buttonAction(_ sender: UIButton) {
        delegate?.didTapOnButton()
    }
}
