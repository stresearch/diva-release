//
//  StatsVerifiedTableViewCell.swift
//  STR
//
//  Created by SRUJAN KUMAR VEERANTI on 14/01/20.
//  
//

import UIKit

class StatsVerifiedTableViewCell: UITableViewCell {

    //MARK: IBOutlets
    @IBOutlet weak var labelVerifiedVideos: UILabel!
    @IBOutlet weak var labelNotVerifiedVideos: UILabel!
    @IBOutlet weak var labelConsentedVideos: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

}
