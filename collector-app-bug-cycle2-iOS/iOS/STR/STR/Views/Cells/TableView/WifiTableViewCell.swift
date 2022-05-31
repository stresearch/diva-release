//
//  WifiTableViewCell.swift
//  STR
//
//  Created by SRUJAN KUMAR VEERANTI on 14/01/20.
//  
//

import UIKit

class WifiTableViewCell: UITableViewCell {

    //MARK: Outlets
    @IBOutlet weak var switchWiFi: UISwitch!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    //MARK: IBActions
    @IBAction func switchAction(_ sender: UISwitch) {
        
    }
}
