//
//  ProjectsViewController.swift
//  STR
//
//  Created by Srujan on 02/01/20.
//  
//

import UIKit
import AWSAppSync

class ProjectsViewController: UITableViewController {
    
    // MARK:- Data Members
    fileprivate let searchController = UISearchController(searchResultsController: nil)
    fileprivate var filteredProjects: [Project] = []
    lazy var projects: [Project] = []
    
    // MARK:- LifeCycle
    override func viewDidLoad() {
        super.viewDidLoad()
        
        setupTableView()
        getCollectorAssignedCollections(token: "", collectionsList: [])
        
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.tabBarController?.tabBar.isHidden = true
        setupNavBar()
        
        for tabBarItem in (self.tabBarController?.tabBar.items)! {
            tabBarItem.title = ""
        }
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        MenuViewController.valselectedIndex = -1
        self.navigationController?.isNavigationBarHidden = false
        
    }
    
    // MARK:- UI Utils
    private func setupTableView(){
        self.tableView.estimatedRowHeight = 100
        self.tableView.rowHeight = UITableView.automaticDimension
        self.tableView.alwaysBounceVertical = false
    }
    
    private func setupNavBar() {
        
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
        
        self.title = "PROJECTS" //Set Title
        self.navigationController?.navigationBar.titleTextAttributes = attributes
        
        self.navigationController?.navigationBar.shouldRemoveShadow(true)
        
        self.navigationItem.leftBarButtonItem = leftItem()
        self.navigationItem.rightBarButtonItem = rightItem()
    }
    
    func leftItem() -> UIBarButtonItem  {
        let button = UIButton.init(type: .custom)
        button.addTarget(self, action: #selector(sideMenuAction(_:)), for: .touchUpInside)
        let imageView = UIImageView(frame: CGRect(x: 0, y: 8, width: 20, height: 20))
        imageView.image = UIImage(named: "back_small")
                
        let buttonView = UIView(frame: CGRect(x: 0, y: 0, width: 30, height: 30))
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
        self.navigationController?.popViewController(animated: true)
    }
    
    @objc func searchBtnAction(_ sender: UIButton) {
        configureSearchController()
    }
        
    private func didOpenSelectVehicleVC(for project: Project) {
        
        ProjectService.instance.currentProject = project
        
        let currentProject = "\(ProjectService.instance.currentProject?.title ?? "")"
        
        UserDefaults.standard.set(currentProject, forKey: UserDefaults.Keys.currentProject.rawValue)
        
        self.tabBarController?.selectedIndex = 1
        self.navigationController?.popViewController(animated: true)
    }
    
    //MARK: IBActions
    @IBAction func closeBtnAction(_ sender: UIButton) {
        self.navigationController?.popViewController(animated: true)
    }
    
    @IBAction func searchBarBtnPressed(_ sender: UIBarButtonItem) {
        configureSearchController()
    }
    
    //MARK: Search Methods
    private func configureSearchController() {
        searchController.delegate = self
        searchController.searchResultsUpdater = self
        searchController.dimsBackgroundDuringPresentation = false
        definesPresentationContext = true
        tableView.tableHeaderView = searchController.searchBar
        searchController.searchBar.becomeFirstResponder()
    }
    
    private func removeSearchController() {
        UIView.animate(withDuration: 0.2) {
            self.tableView.tableHeaderView = UIView(frame: CGRect(origin: CGPoint.zero, size: CGSize(width: self.tableView.frame.width, height: 25)))
            self.tableView.layoutIfNeeded()
            self.view.layoutIfNeeded()
        }
    }
    
    private func filterSearchController(_ searchBar: UISearchBar) {
        let searchText = searchBar.text ?? ""
        
        // filter projectlist by element and text
        filteredProjects = projects.filter { project in
            let isMatchingSearchText = project.title.lowercased().contains(searchText.lowercased()) || searchText.lowercased().count == 0
            return isMatchingSearchText
        }
        tableView.reloadData()
    }
}

// MARK:- Table View Delegates
extension ProjectsViewController {
    
    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return ProjectInfoTableViewCell.rowHeight
    }
    
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        ConsentResponse.instance.subjectEmail = ""
        
        ProjectService.instance.isPracticeProject = false
        didOpenSelectVehicleVC(for: self.projects[indexPath.row])
        
        if self.searchController.isActive {
            didOpenSelectVehicleVC(for: self.filteredProjects[indexPath.row])
        } else {
            didOpenSelectVehicleVC(for: self.projects[indexPath.row])
        }
    }
}

// MARK:- Table View Data Source
extension ProjectsViewController{
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return searchController.isActive ? filteredProjects.count : projects.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        if let cell = tableView
            .dequeueReusableCell(withIdentifier: ProjectInfoTableViewCell.reuseIdentifier, for: indexPath) as? ProjectInfoTableViewCell {
                
            let project = searchController.isActive ? self.filteredProjects[indexPath.row] : self.projects[indexPath.row]
            cell.configureView(projectName: project.title, totalActivities: project.collections.count, asset: .project)
            
            return cell
        }
        return UITableViewCell()
    }
}

//MARK: UISearchResultsUpdating Delegate
extension ProjectsViewController : UISearchResultsUpdating {
    
    func updateSearchResults(for searchController: UISearchController) {
        
        filterSearchController(searchController.searchBar)
    }
}

//MARK: UISearchControllerDelegate Methods
extension ProjectsViewController: UISearchControllerDelegate {
    
    func didDismissSearchController(_ searchController: UISearchController) {
        removeSearchController()
    }
    
    func didPresentSearchController(_ searchController: UISearchController) {
        searchController.searchBar.becomeFirstResponder()
    }
}

//MARK: API Calls
extension ProjectsViewController {
    
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
                    self.presentDefaultAlertWithTitle(title: nil, message: LocalizableString.collectorProjectsMappingEmptyList.localizedString, animated: true, completion: nil)
                    return
                }
                
                collections.append(contentsOf: items)
                
                if token != nil {
                    self.getCollectorAssignedCollections(token: token ?? "", collectionsList: collections)
                } else {
                    collections.forEach { (item) in
                        if let collection = self.projects.firstIndex(where: {$0.title == item.projectName ?? ""}) {
                            self.projects[collection].collections.append(item)
                        } else {
                            let collection = Project(title: item.projectName ?? "", collections: [item])
                            self.projects.append(collection)
                        }
                    }
                    
                    self.projects.sort(by: {$0.title.lowercased() < $1.title.lowercased()})
                    
                    if let projectName = UserDefaults.standard.value(forKey: UserDefaults.Keys.currentProject.rawValue) as? String {
                        if let project = self.projects.first(where: {$0.title == projectName}) {
                            ProjectService.instance.currentProject = project
                        }
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
