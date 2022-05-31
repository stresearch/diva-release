//
//  ProjectInfoTableViewCell.swift
//  STR
//
//  Created by Srujan on 02/01/20.
//  
//

import UIKit

class ProjectInfoTableViewCell: UITableViewCell {
    
    // MARK:- Outlets
    @IBOutlet weak var backgroundColorView: UIView!
    @IBOutlet weak var folderView: UIView!
    @IBOutlet weak var iconImageView: UIImageView!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var activityCountLabel: UILabel!
    
    static let rowHeight: CGFloat = 96.0
    func configureView(projectName: String, totalActivities: Int, asset: Asset) {
        let currentProject = ProjectService.instance.currentProject
        
        if projectName == currentProject?.title && (ProjectService.instance.isPracticeProject == false || ProjectService.instance.isPracticeProject == nil) {
            self.backgroundColorView.backgroundColor = UIColor.appColor(.main)
            self.iconImageView.tintColor = UIColor.appColor(.main)
            self.folderView.backgroundColor = .white
            self.nameLabel.textColor = .white
            self.activityCountLabel.textColor = .white
        }
        else {
            self.backgroundColorView.backgroundColor = .white
            self.iconImageView.tintColor = .white
            self.folderView.backgroundColor = UIColor.appColor(.main)
            self.nameLabel.textColor = .black
            self.activityCountLabel.textColor = .black
        }
        
        self.nameLabel.text = projectName
        if totalActivities > 0 {
            self.activityCountLabel.text = (totalActivities > 1) ? "\(totalActivities) Collections" : "\(totalActivities) Collection"
            self.activityCountLabel.isHidden = false
        } else {
            self.activityCountLabel.isHidden = true
        }
        
        self.iconImageView.image = UIImage(asset: asset)
        
    }
    
}
