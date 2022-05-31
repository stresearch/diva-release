//
//  MenuViewController.swift
//  STR
//
//  Created by Govind Prasad  on 20/5/20.
//  
//

import UIKit

class MenuViewController: UIViewController {
    
    @IBOutlet weak var selectionTableViewHeader: UILabel!
    @IBOutlet weak var versionTermsLbl: UILabel!
    @IBOutlet weak var accountNameLbl: UILabel!
    @IBOutlet weak var emailLbl: UILabel!
    @IBOutlet weak var tableView: UITableView! {
        didSet {
            tableView.dataSource = self
            tableView.delegate = self
            tableView.separatorStyle = .none
        }
    }
    @IBOutlet weak var selectionMenuTrailingConstraint: NSLayoutConstraint!
    
    var isDarkModeEnabled = false
    private var themeColor = UIColor.white
    static var valselectedIndex = -1
    let arrTitle = ["Projects", "Profile", "Help", "About"]
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        configureView()
        
        sideMenuController?.cache(viewControllerGenerator: {
            self.storyboard?.instantiateViewController(withIdentifier: "ThirdViewController")
        }, with: "2")
        
        sideMenuController?.delegate = self
        
    }
    
    override func viewWillAppear(_ animated: Bool) {
        //print("1MenuViewController.valselectedIndex---9\(MenuViewController.valselectedIndex)")
        setVersion()
        tableView.reloadData()
        
    }
    
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        
        let sidemenuBasicConfiguration = MenuController.preferences.basic
        let showPlaceTableOnLeft = (sidemenuBasicConfiguration.position == .under) != (sidemenuBasicConfiguration.direction == .right)
        selectionMenuTrailingConstraint.constant = showPlaceTableOnLeft ? MenuController.preferences.basic.menuWidth - size.width : 0
        view.layoutIfNeeded()
    }
    
    private func configureView() {
        selectionMenuTrailingConstraint.constant = 0
        
        let sidemenuBasicConfiguration = MenuController.preferences.basic
        let showPlaceTableOnLeft = (sidemenuBasicConfiguration.position == .under) != (sidemenuBasicConfiguration.direction == .right)
        if showPlaceTableOnLeft {
            //            selectionMenuTrailingConstraint.constant = MenuController.preferences.basic.menuWidth - view.frame.width
        }
        
        view.backgroundColor = themeColor
        tableView.backgroundColor = themeColor
        tableView.alwaysBounceVertical = false
    }
    
    private func setVersion() {
        let user = Collector.currentCollector
        accountNameLbl.text = (user.firstName ?? "") + " " + (user.lastName ?? "")
        emailLbl.text = user.email ?? ""
        
        self.versionTermsLbl.text = "Version " + Utilities.getAppVersion() + "  |  " //+ " | Terms of Use"
    }
    
    @IBAction func termsBtnAction(_ sender: UIButton) {
        if let url = URL(string: LocalizableString.termsURL.localizedString) {
            UIApplication.shared.open(url)
        }
    }
}

//MARK: Table Delegate and Data Source
extension MenuViewController: UITableViewDelegate, UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return 4 // JEBYRNE
    }
    
    // swiftlint:disable force_cast
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "Cell", for: indexPath) as! SelectionCell
        cell.contentView.backgroundColor = themeColor
        let row = indexPath.row
        cell.titleLabel?.text = arrTitle[row]
        
        cell.titleLabel.layer.sublayerTransform = CATransform3DMakeTranslation(16, 0, 0)
        
        if indexPath.row == MenuViewController.valselectedIndex {
            cell.titleLabel.backgroundColor = UIColor.appColor(.main)
            cell.titleLabel.textColor = .white
        }
        else {
            cell.titleLabel.backgroundColor = .white
            cell.titleLabel.textColor = .black
        }
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let row = indexPath.row
        if row != 1 {
            MenuViewController.valselectedIndex = row
        }
        else {
            MenuViewController.valselectedIndex = -1
        }
        if row < 2 {  // JEBYRNE
        sideMenuController?.setContentViewController(with: "\(row)", animated: Preferences.shared.enableTransitionAnimation)
        sideMenuController?.hideMenu()
        
        if let identifier = sideMenuController?.currentCacheIdentifier() {
            print("[Example] View Controller Cache Identifier: \(identifier)")
        }
        } else {
            if row == 2 {  // JEBYRNE
            if let url = URL(string: LocalizableString.faqURL.localizedString) {
                UIApplication.shared.open(url)
                sideMenuController?.hideMenu()
            }
        }
            if row == 3 {  // JEBYRNE
            if let url = URL(string: LocalizableString.aboutURL.localizedString) {
                UIApplication.shared.open(url)
                sideMenuController?.hideMenu()
            }
            }
        
        }}
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 74
    }
}

//MARK: SideMenuControllerDelegate
extension MenuViewController: SideMenuControllerDelegate {
    func sideMenuController(_ sideMenuController: MenuController,
                            animationControllerFrom fromVC: UIViewController,
                            to toVC: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        return BasicTransitionAnimator(options: .transitionFlipFromLeft, duration: 0.6)
    }
    
    func sideMenuController(_ sideMenuController: MenuController, willShow viewController: UIViewController, animated: Bool) {
        print("[Example] View controller will show [\(viewController)]")
    }
    
    func sideMenuController(_ sideMenuController: MenuController, didShow viewController: UIViewController, animated: Bool) {
        print("[Example] View controller did show [\(viewController)]")
    }
    
    func sideMenuControllerWillHideMenu(_ sideMenuController: MenuController) {
        print("[Example] Menu will hide")
    }
    
    func sideMenuControllerDidHideMenu(_ sideMenuController: MenuController) {
        print("[Example] Menu did hide.")
    }
    
    func sideMenuControllerWillRevealMenu(_ sideMenuController: MenuController) {
        print("[Example] Menu will reveal.")
    }
    
    func sideMenuControllerDidRevealMenu(_ sideMenuController: MenuController) {
        print("[Example] Menu did reveal.")
    }
}

//MARK: SelectionCell
class SelectionCell: UITableViewCell {
    @IBOutlet weak var titleLabel: UILabel!
}
