//
//  EditAccountTableViewCell.swift
//  STR
//
//  Created by SRUJAN KUMAR VEERANTI on 14/01/20.
//  
//

import UIKit

protocol EditAccountTableViewCellDelegate: class {
    func didTapOnEditAccount(for row: TableRow)
}

class EditAccountTableViewCell: UITableViewCell {

    //MARK: Outlets
    @IBOutlet weak var labelAccountName: UILabel!
    @IBOutlet weak var labelEmail: UILabel!
    @IBOutlet weak var buttonEdit: UIButton!
    
    //MARK: Data Members
    weak var delegate: EditAccountTableViewCellDelegate?
    var currentRow: TableRow!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    //MARK: IBActions
    @IBAction func editBtnActions(_ sender: UIButton) {
      delegate?.didTapOnEditAccount(for: currentRow)
    }
}
