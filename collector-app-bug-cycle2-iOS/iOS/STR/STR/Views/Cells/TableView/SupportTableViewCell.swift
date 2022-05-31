//
//  SupportTableViewCell.swift
//  STR
//
//  Created by SRUJAN KUMAR VEERANTI on 14/01/20.
//  
//

import UIKit

protocol SupportTableViewCellDelegate: class {
    func didTapOnButtonAction(for row: TableRow)
    func didTapOnConnectButtonAction(for row: TableRow, tag: Int)
}

class SupportTableViewCell: UITableViewCell {

    //MARK: Outlets
    @IBOutlet weak var buttonTitle: UIButton!
    @IBOutlet weak var buttonConnect: UIButton!
    
    weak var delegate: SupportTableViewCellDelegate?
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
    @IBAction func buttonAction(_ sender: UIButton) {
        delegate?.didTapOnButtonAction(for: currentRow)
    }
    
    @IBAction func buttonConnectAction(_ sender: UIButton) {
        delegate?.didTapOnConnectButtonAction(for: currentRow, tag: buttonConnect.tag)
    }
}
