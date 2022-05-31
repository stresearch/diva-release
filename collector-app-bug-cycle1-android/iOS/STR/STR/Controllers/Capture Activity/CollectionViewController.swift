//
//  ActivityTypeViewController.swift
//  STR
//
//  Created by Srujan on 02/01/20.
//  
//

import UIKit

class CollectionViewController: UITableViewController {
    
    fileprivate let searchController = UISearchController(searchResultsController: nil)
    fileprivate var filteredCollections: ListStrCollectionsAssignment = []
    @IBOutlet weak var searchBar: UISearchBar!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        MenuController.panGestureRecognizer?.isEnabled = true
        self.navigationController?.navigationBar.isHidden = false
        self.tabBarController?.tabBar.isHidden = false
        self.navigationController?.setNavigationBarHidden(false, animated: false)
        getCollectorAssignedCollections(token: "", collectionsList: [])
        
        for tabBarItem in (self.tabBarController?.tabBar.items)! {
            tabBarItem.title = ""
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

    }
    
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        
    }
    
    // MARK:- UI Utils
    
    private func setupUI() {
        
        let shadow = NSShadow()
        shadow.shadowColor = UIColor(red: 1.0, green: 1.0, blue: 1.0, alpha: 1.0)
        shadow.shadowOffset = CGSize(width: 0, height: 1)
        let color : UIColor = UIColor.darkText
        let titleFont : UIFont = UIFont.systemFont(ofSize: 18, weight: .bold)
        
        let attributes = [
            NSAttributedString.Key.foregroundColor : color,
            NSAttributedString.Key.shadow : shadow,
            NSAttributedString.Key.font : titleFont
        ]
        
        self.title = "COLLECTIONS" //Set Title
        self.navigationController?.navigationBar.titleTextAttributes = attributes
        
        self.navigationController?.navigationBar.shouldRemoveShadow(true)
        
        self.navigationItem.leftBarButtonItem = leftItem()
        self.navigationItem.rightBarButtonItem = rightItem()
        setupTableView()
    }
    
    private func setupTableView() {
        self.tableView.estimatedRowHeight = 100
        self.tableView.rowHeight = UITableView.automaticDimension
        self.tableView.alwaysBounceVertical = false
    }
    
    func leftItem() -> UIBarButtonItem  {
        let button = UIButton.init(type: .custom)
        button.addTarget(self, action: #selector(sideMenuAction(_:)), for: .touchUpInside)
        let imageView = UIImageView(frame: CGRect(x: 0, y: 5, width: 15, height: 15))
        imageView.image = UIImage(named: "side_Menu")
        
        let buttonView = UIView(frame: CGRect(x: 0, y: 0, width: 25, height: 25))
        button.frame = buttonView.frame
        buttonView.addSubview(button)
        buttonView.addSubview(imageView)
        let barButton = UIBarButtonItem.init(customView: buttonView)
        
        return barButton
    }
    
    func rightItem() -> UIBarButtonItem {
        let button = UIButton.init(type: .custom)
        button.addTarget(self, action: #selector(searchBtnAction(_:)), for: .touchUpInside)
        let imageView = UIImageView(frame: CGRect(x: 0, y: 5, width: 20, height: 20))
        imageView.image = UIImage(named: "search")
        
        let buttonView = UIView(frame: CGRect(x: 0, y: 0, width: 30, height: 30))
        button.frame = buttonView.frame
        buttonView.addSubview(button)
        buttonView.addSubview(imageView)
        let barButton = UIBarButtonItem.init(customView: buttonView)
        
        return barButton
    }
    
    @objc func sideMenuAction(_ sender:AnyObject) {
        sideMenuController?.revealMenu()
    }
    
    @objc func searchBtnAction(_ sender: UIButton) {
        
        configureSearchController()
    }
     
    private func showActivities(selectedCollection: ListStrCollectionsAssignmentItem) {
        ProjectService.instance.currentCollection = selectedCollection
        let captureActivityStoryBoard = UIStoryboard(.activity)
        let activityVC = captureActivityStoryBoard.instantiateViewController(withIdentifier: "ActivityViewController") as! ActivityViewController
        
        activityVC.hidesBottomBarWhenPushed = true
        self.navigationController?.pushViewController(activityVC, animated: true)
    }
    
    // MARK:- Actions
    
    @IBAction func backBtnPressed(_ sender: UIButton) {
        sideMenuController?.revealMenu()
    }
    
    //MARK: Search Methods
    
    private func configureSearchController() {
        searchController.delegate = self
        searchController.searchResultsUpdater = self
        searchController.dimsBackgroundDuringPresentation = false
        definesPresentationContext = true
        //searchController.hidesNavigationBarDuringPresentation = false
        tableView.tableHeaderView = searchController.searchBar
        searchController.searchBar.becomeFirstResponder()
    }
    
    private func removeSearchController() {
        UITableView.animate(withDuration: 0.2) {
            self.tableView.tableHeaderView = UIView(frame: CGRect(origin: CGPoint.zero, size: CGSize(width: self.tableView.frame.width, height: 25)))
            self.tableView.layoutIfNeeded()
        }
        
    }
    
    private func filterSearchController(_ searchBar: UISearchBar) {
        let searchText = searchBar.text ?? ""
        
        // filter collectionlist by element and text
        filteredCollections = ProjectService.instance.currentProject?.collections.filter { collection in
            let isMatchingSearchText = collection.collectionName?.lowercased().contains(searchText.lowercased()) ?? false || searchText.lowercased().count == 0
            return isMatchingSearchText
            } ?? []
        
        tableView.reloadData()
    }
    
}

// MARK:- Delegates
extension CollectionViewController {
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        if ProjectService.instance.isPracticeProject ?? false {
            let captureActivityStoryBoard = UIStoryboard(.activity)
            let activityVC = captureActivityStoryBoard.instantiateViewController(withIdentifier: "ActivityViewController") as! ActivityViewController
            
            activityVC.hidesBottomBarWhenPushed = true
            self.navigationController?.pushViewController(activityVC, animated: true)
        }
        else {
            
            if self.searchController.isActive {
                self.showActivities(selectedCollection: filteredCollections[indexPath.row])
            } else {
                self.showActivities(selectedCollection: (ProjectService.instance.currentProject?.collections[indexPath.row])!)
            }
            
            
        }
    }
}

// MARK:- DataSource
extension CollectionViewController {
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if ProjectService.instance.isPracticeProject ?? false {
            return 1
        }
        return searchController.isActive ? filteredCollections.count : ProjectService.instance.currentProject?.collections.count ?? 0
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = tableView
            .dequeueReusableCell(withIdentifier: ActivityNameTableViewCell.reuseIdentifier, for: indexPath) as? ActivityNameTableViewCell {
            if ProjectService.instance.isPracticeProject ?? false {
                cell.nameLbl.text = "Practice"
                return cell
            }
            if searchController.isActive {
                cell.nameLbl.text = filteredCollections[indexPath.row].collectionName
            } else {
                if let project = ProjectService.instance.currentProject, project.collections.count > 0 {
                    cell.nameLbl.text = project.collections[indexPath.row].collectionName ?? ""
                }
            }
            
            return cell
        }
        return UITableViewCell()
    }
}

//MARK: UISearchResultsUpdating Delgate Methods
extension CollectionViewController : UISearchResultsUpdating {
    
    func updateSearchResults(for searchController: UISearchController) {
        
        filterSearchController(searchController.searchBar)
    }
}
//MARK: UISearchControllerDelegate Methods
extension CollectionViewController: UISearchControllerDelegate {
    
    func didDismissSearchController(_ searchController: UISearchController) {
        removeSearchController()
    }
    
    func didPresentSearchController(_ searchController: UISearchController) {
        searchController.searchBar.becomeFirstResponder()
    }
}

// MARK:- APIs
extension CollectionViewController {
    
    private func getCollectorAssignedCollections(token: String, collectionsList: ListStrCollectionsAssignment) {
        
        self.showProgress()
        self.tabBarController?.tabBar.isUserInteractionEnabled = false
        self.view.isUserInteractionEnabled = false
        
        var collections: ListStrCollectionsAssignment = collectionsList
        
        ProjectsAPI.getCollectorCollections(token: token) { [weak self] (result, error, token) in
            guard let self = self else { return }
            if let err = error {
                self.hideProgress()
                self.tabBarController?.tabBar.isUserInteractionEnabled = true
                self.view.isUserInteractionEnabled = true
                
                self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
            } else if let items = result {
                
                guard items.count > 0 else {
                    self.hideProgress()
                    self.tabBarController?.tabBar.isUserInteractionEnabled = true
                    self.view.isUserInteractionEnabled = true 
                    self.presentDefaultAlertWithTitle(title: nil, message: LocalizableString.collectorCollectionsMappingEmptyList.localizedString, animated: true, completion: nil)
                    return
                }
                
                collections.append(contentsOf: items)
                
                if token != nil {
                    self.getCollectorAssignedCollections(token: token ?? "", collectionsList: collections)
                } else {
                    
                    if let projectName = UserDefaults.standard.value(forKey: UserDefaults.Keys.currentProject.rawValue) as? String {
                        var currentCollections = collections.filter({$0.projectName == projectName})
                        
                        if currentCollections.count == 0 {
                            currentCollections = collections.filter({$0.projectName == collections.first?.projectName})
                        }
                        
                        let project = Project(title: currentCollections.first?.projectName ?? "", collections: currentCollections)
                        ProjectService.instance.currentProject = project
                        UserDefaults.standard.set(project.title, forKey: UserDefaults.Keys.currentProject.rawValue)
                    } else {
                        let currentCollections = collections.filter({$0.projectName == collections.first?.projectName})
                        let project = Project(title: currentCollections.first?.projectName ?? "", collections: currentCollections)
                        ProjectService.instance.currentProject = project
                        UserDefaults.standard.set(project.title, forKey: UserDefaults.Keys.currentProject.rawValue)
                    }
                    
                    self.hideProgress()
                    self.tabBarController?.tabBar.isUserInteractionEnabled = true
                    self.view.isUserInteractionEnabled = true
                    self.tableView.reloadData()
                }
            }
        }
    }
}
