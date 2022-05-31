//
//  LeftMenuViewController.swift
//  STR
//
//  Created by admin on 15/5/20.
//  Copyright © 2020 BTC. All rights reserved.
//

import UIKit
//import SlideMenuController

enum LeftMenu: Int {
  case studyList = 0
  case resources
  case profile_reachOut
  case reachOut_signIn
  case signup
}

protocol LeftMenuProtocol: class {
  func changeViewController(_ menu: LeftMenu)
}

class LeftMenuViewController: UIViewController, LeftMenuProtocol {

  // MARK: - Outlets
  @IBOutlet weak var tableView: UITableView!

  @IBOutlet weak var labelVersion: UILabel!
  @IBOutlet weak var labelProductName: UILabel!
    @IBOutlet weak var labelPoweredBy: UILabel!
  @IBOutlet weak var tableHeaderView: UIView!
  @IBOutlet weak var tableFooterView: UIView!
  @IBOutlet weak var buttonSignOut: UIButton?

  // MARK: - Properties
  lazy var menus: [[String: Any]] = [
    [
      "menuTitle": "Home",
      "iconName": "home_menu1-1",
      "menuType": LeftMenu.studyList,
    ],

    [
      "menuTitle": "Resources",
      "iconName": "resources_menu1",
      "menuType": LeftMenu.resources,
    ],
  ]

  /// Standalone
  var studyTabBarController: UITabBarController!

  var studyHomeViewController: UINavigationController!

  /// Gateway & standalone
  var studyListViewController: UINavigationController!

  var notificationController: UIViewController!
  var resourcesViewController: UINavigationController!
  var profileviewController: UIViewController!
  var nonMenuViewController: UIViewController!
  var reachoutViewController: UINavigationController!
  var signInViewController: UINavigationController!
  var signUpViewController: UINavigationController!

  var shouldAllowToGiveFeedback = true

  required init?(coder aDecoder: NSCoder) {
    super.init(coder: aDecoder)
  }

  // MARK: - ViewController Lifecycle
  override func viewDidLoad() {
    super.viewDidLoad()

    self.view.isHidden = true
    
    self.createLeftmenuItems()

    var infoDict: NSDictionary?
    if let path = Bundle.main.path(forResource: "Info", ofType: "plist") {
      infoDict = NSDictionary(contentsOfFile: path)
    }
    
    labelProductName.text = "Branding.NavigationTitleName"

    self.tableView.separatorColor = UIColor(
      red: 224 / 255,
      green: 224 / 255,
      blue: 224 / 255,
      alpha: 1.0
    )
    setupStandaloneMenu()
    
    self.labelVersion.text = "V" + "\(Utilities.getAppVersion())"
  }

  override func viewDidAppear(_ animated: Bool) {
    super.viewDidAppear(animated)
    self.view.isHidden = false
  }
    

  // MARK: - UI Utils

  /// This method will setup the Menu in case of Standalone app.
  final private func setupStandaloneMenu() {

    let studyStoryBoard = UIStoryboard.init(name: "Main", bundle: Bundle.main)
    // for standalone
    self.studyTabBarController =
      studyStoryBoard.instantiateViewController(
        withIdentifier: "STRTabBarController"
      )
      as! STRTabBarController

    let storyboard = UIStoryboard(name: "Main", bundle: nil)

    self.studyListViewController =
      storyboard.instantiateViewController(
        withIdentifier: String(describing: ProjectsViewController.classForCoder())
      )
      as? UINavigationController

    self.studyHomeViewController =
      studyStoryBoard.instantiateViewController(
        withIdentifier: String(describing: "ProjectsViewController")
      )
      as? UINavigationController  // for standalone

    self.notificationController =
      storyboard.instantiateViewController(
        withIdentifier: String(describing: ProjectsViewController.classForCoder())
      )
      as? UINavigationController

    self.resourcesViewController =
      storyboard.instantiateViewController(
        withIdentifier: String(describing: ProjectsViewController.classForCoder())
      )
      as? UINavigationController

    self.profileviewController =
      storyboard.instantiateViewController(
        withIdentifier: String(describing: ProfileViewController.classForCoder())
      )
      as? UINavigationController

    self.reachoutViewController =
      storyboard.instantiateViewController(
        withIdentifier: String(describing: UINavigationController.classForCoder())
      )
      as? UINavigationController

  }

  /// Used to create Left menu items.
  func createLeftmenuItems() {
    
    menus = [
      [
        "menuTitle": "Home",
        "iconName": "home_menu1-1",
        "menuType": LeftMenu.studyList,
      ],
    ]

      menus.append(
        [
          "menuTitle": "Sign In",
          "iconName": "signin_menu1",
          "menuType": LeftMenu.reachOut_signIn,
        ])

      menus.append(
        [
          "menuTitle": "New User?",
          "iconName": "newuser_menu1",
          "subTitle": "Sign up",
          "menuType": LeftMenu.signup,
        ])
      self.buttonSignOut?.isHidden = true
    

    // Setting proportion height of the header and footer view
    var height: CGFloat? = 0.0
    height = (UIScreen.main.bounds.size.height - CGFloat(menus.count * 70)) / 2

    self.tableHeaderView.frame.size = CGSize(
      width: self.tableHeaderView!.frame.size.width,
      height: height!
    )
    self.tableFooterView.frame.size = CGSize(
      width: self.tableFooterView!.frame.size.width,
      height: height!
    )
    self.tableView.frame.size = CGSize(
      width: self.tableView.frame.width,
      height: UIScreen.main.bounds.size.height
    )

    self.tableView.reloadData()

  }

  /// Used to set the initial data for new user
  func setInitialData() {
    menus.append(
      [
        "menuTitle": "Sign In",
        "iconName": "signin_menu1",
      ])

    menus.append(
      [
        "menuTitle": "New User?",
        "iconName": "newuser_menu1",
        "subTitle": "Sign up",
      ])
    self.tableView.tableFooterView?.isHidden = true


    /// Setting proportion height of the header and footer view
    let height = UIScreen.main.bounds.size.height * (220.0 / 667.0)  // calculate new height
    self.tableView.tableHeaderView?.frame.size = CGSize(
      width: self.tableView.tableHeaderView!.frame.size.width,
      height: height
    )
    self.tableView.tableFooterView?.frame.size = CGSize(
      width: self.tableView.tableFooterView!.frame.size.width,
      height: height
    )
    self.tableView.frame.size = CGSize(
      width: self.tableView.frame.width,
      height: UIScreen.main.bounds.size.height
    )
    self.tableView.reloadData()
  }

  /// Used to change the view controller when clicked from the left menu.
  /// - Parameter menu:  Accepts the data from enum LeftMenu
  func changeViewController(_ menu: LeftMenu) {
    
    switch menu {
    case .studyList:

        self.slideMenuController()?.changeMainViewController(
          self.studyListViewController,
          close: true
        )

    case .resources:
      self.slideMenuController()?.changeMainViewController(
        self.resourcesViewController,
        close: true
      )

    case .profile_reachOut:
        // go to ReachOut screen
        self.slideMenuController()?.changeMainViewController(
          self.reachoutViewController,
          close: true
        )

    case .reachOut_signIn:
        

        // go sign in
        self.slideMenuController()?.changeMainViewController(
          self.signInViewController,
          close: true
        )
        
    case .signup:
        self.slideMenuController()?.changeMainViewController(
        self.signUpViewController,
        close: true
      )

    }
  }
    
    fileprivate func removeViewController(_ viewController: UIViewController?) {
        if let _viewController = viewController {
            _viewController.view.layer.removeAllAnimations()
            _viewController.willMove(toParent: nil)
            _viewController.view.removeFromSuperview()
            _viewController.removeFromParent()
        }
    }

  // MARK: - Button Actions

  /// Signout button clicked.
  /// - Parameter sender: Instance of UIButton.
  @IBAction func buttonActionSignOut(_ sender: UIButton) {

  }

}

// MARK: - UITableView Delegate
extension LeftMenuViewController: UITableViewDelegate {

  func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {

    if let menu = LeftMenu(rawValue: indexPath.row) {
      switch menu {
      case .studyList, .resources, .profile_reachOut, .reachOut_signIn, .signup:
        return 70.0
      }
    }
    return 0
  }

  func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

    tableView.deselectRow(at: indexPath, animated: true)

    if let menu = menus[indexPath.row]["menuType"] as? LeftMenu {
      self.changeViewController(menu)
    }
  }

  func scrollViewDidScroll(_ scrollView: UIScrollView) {
    if self.tableView == scrollView {

    }
  }
}

// MARK: - UITableView DataSource
extension LeftMenuViewController: UITableViewDataSource {

  func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
    return menus.count
  }

  func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

    let dict: [String: Any]? = menus[indexPath.row]

    if dict?["subTitle"] != nil {
      var cell: LeftMenuCell?
      cell =
        tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath)
        as? LeftMenuCell
      cell?.populateCellData(data: menus[indexPath.row])
      return cell!
    } else {
      var cell: LeftMenuResourceTableViewCell?
      cell =
        tableView.dequeueReusableCell(
          withIdentifier: "LeftMenuResourceCell",
          for: indexPath
        )
        as? LeftMenuResourceTableViewCell
      cell?.populateCellData(data: menus[indexPath.row])
      return cell!
    }
  }
}
