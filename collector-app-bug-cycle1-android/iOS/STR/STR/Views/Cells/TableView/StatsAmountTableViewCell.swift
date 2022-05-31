//
//  StatsAmountTableViewCell.swift
//  STR
//
//  Created by SRUJAN KUMAR VEERANTI on 14/01/20.
//  
//

import UIKit

class StatsAmountTableViewCell: UITableViewCell {

    //MARK: IBOutlets
    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelAmount: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

}
