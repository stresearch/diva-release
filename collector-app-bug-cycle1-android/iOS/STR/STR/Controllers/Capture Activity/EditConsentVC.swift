//
//  EditConsentVC.swift
//  STR
//
//  Created by GovindPrasad on 8/5/20.
//  
//

import UIKit

enum EditConsentVCTableRow: TableRow {
    
    case datasetRelease, faceDetection, retainmentTime
    
    var title: String {
        switch self {
        case .datasetRelease: return LocalizableString.datasetRelease.localizedString
        case .faceDetection: return LocalizableString.faceDetection.localizedString
        case .retainmentTime: return LocalizableString.retainmentTime.localizedString
        }
    }
    
    var key: String {
        switch self {
        case .datasetRelease: return LocalizableString.datasetReleaseKey.localizedString
        case .faceDetection: return LocalizableString.faceDetectionKey.localizedString
        case .retainmentTime: return LocalizableString.retainmentTime.localizedString
        }
    }
}


class EditConsentVC: UIViewController {
    
    //MARK: Outlets
    @IBOutlet weak var lblDescriptionLb: UILabel!
    @IBOutlet weak var buttonBack: UIButton!
    @IBOutlet weak var buttonSave: UIButton!
    @IBOutlet weak var buttonRetakeVideoConsent: UIButton!
    @IBOutlet weak var tableView: UITableView!
    
    fileprivate let tableRows: [EditConsentVCTableRow] = [
        .datasetRelease, .faceDetection, .retainmentTime
    ]
    
    var arrDropDown0: [String] = []
    var arrDropDown1: [String] = []
    var arrDropDown2: [String] = []
    lazy var listQuestions: [ListStrConsentQuestionnairesQuery.Data.ListStrConsentQuestionnaire.Item]? = []
    var arrSelectedID: [Int] = [0,0,0,0,0]
    var arrOfQuestionResponse: [JSONDictionary] = []
    var consentResponse: SubjectByStrSubjectEmailQuery.Data.SubjectByStrSubjectEmail.Item?
    
    var arrPlaceholder = ["Dataset Release", "Face recognition usage", "Retention"]
    lazy var subjectCollectorEmail = ""
    
    private var editConsent: EditConsent?
    
    deinit {
        Log("\(self) I'm gone ") // Keep eye on this
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        setupTableView()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        if (consentResponse != nil)  {
            self.stringJsonDecode(str: consentResponse?.consentResponse ?? "")
        } else {
            self.requestForConsentResponse()
        }
    }
    
    // MARK:- UI Utils
    private func setupTableView() {
        self.tableView.dataSource = self
        self.tableView.delegate = self
        tableView.alwaysBounceVertical = false
    }
    
    //MARK: IBActions
    @IBAction func backBtnPressed(_ sender: UIButton) {
        self.navigationController?.popViewController(animated: true)
    }
    
    @IBAction func saveBtnPressed(_ sender: UIButton) {
        updateConsentResponse()
        
    }
    
    @IBAction func retakeVideoBtnAction(_ sender: UIButton) {
        let captureActivityStoryBoard = UIStoryboard(.activity)
        let consentVC = captureActivityStoryBoard.instantiateViewController(withIdentifier: "RecordConsentVC") as! RecordConsentVC
        ConsentResponse.instance.isEditConsent = true
        ConsentResponse.instance.subjectEmail = Collector.currentCollector.email
        consentVC.hidesBottomBarWhenPushed = true
        self.navigationController?.pushViewController(consentVC, animated: true)
    }
}

extension EditConsentVC: UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if arrDropDown0.count > 0 {
            return tableRows.count
        }
        return 0
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        if let cell = tableView.dequeueReusableCell(withIdentifier: SingleDropDownTableViewCell.reuseIdentifier, for: indexPath) as? SingleDropDownTableViewCell {
            let curentRow = tableRows[indexPath.row]
            cell.placeholderLbl.backgroundColor = .white
            cell.placeholderLbl.layer.sublayerTransform = CATransform3DMakeTranslation(5, 0, 5)
            
            cell.currentRow = curentRow
            
            cell.placeholderLbl.text = curentRow.title
            
            if curentRow == .datasetRelease {
                cell.textField.optionArray = arrDropDown0
                cell.textField.selectedIndex = arrSelectedID[indexPath.row]
                cell.textField.text = arrDropDown0[cell.textField.selectedIndex ?? 0]
                cell.textField.layer.sublayerTransform = CATransform3DMakeTranslation(15, 0, 0)
            }
            else if curentRow == .faceDetection {
                cell.textField.selectedIndex = arrSelectedID[indexPath.row]
                cell.textField.text = arrDropDown1[cell.textField.selectedIndex ?? 0]
                cell.textField.optionArray = arrDropDown1
                cell.textField.layer.sublayerTransform = CATransform3DMakeTranslation(15, 0, 0)
            }
            else if curentRow == .retainmentTime {
                cell.textField.selectedIndex = arrSelectedID[indexPath.row]
                cell.textField.text = arrDropDown2[cell.textField.selectedIndex ?? 0]
                cell.textField.optionArray = arrDropDown2
                cell.textField.layer.sublayerTransform = CATransform3DMakeTranslation(15, 0, 0)
            }
            
            cell.textField.didSelect("") { [weak self] (selectedText , index ,id) in
                guard let self = self else { return }
                self.arrSelectedID[indexPath.row] = index
                
                if let row = self.editConsent?.response.firstIndex(where: {$0.qCategoryResponse == selectedText && $0.qCategory == curentRow.key}) {
                    self.editConsent?.response[row].response = "true"
                }
                
                if let row = self.editConsent?.response.firstIndex(where: {$0.qCategoryResponse != selectedText && $0.qCategory == curentRow.key}) {
                    self.editConsent?.response[row].response = "false"
                }
            }
            
            return cell
        }
        return UITableViewCell()
    }
}

extension EditConsentVC: UITableViewDelegate {
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 90
    }
    
}

// MARK:- Custom Functions
extension EditConsentVC {
    
    func toJSON(array: [[String: String]]) throws -> String {
        let data = try JSONSerialization.data(withJSONObject: array, options: [])
        return String(data: data, encoding: .utf8)!
    }
    
    func fromJSON(string: String) throws -> [[String: Any]] {
        let data = string.data(using: .utf8)!
        guard let jsonObject = try JSONSerialization.jsonObject(with: data, options: []) as? [AnyObject] else {
            throw NSError(domain: NSCocoaErrorDomain, code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid JSON"])
        }
        return jsonObject.map { $0 as! [String: Any] }
    }
    
    func stringJsonDecode(str: String) {
        
        let val = str.convertStringToDictionary()!
        
        if var consent = EditConsent.getResponse(jsonObject: val) {
            consent.response = consent.response.sorted(by: {Int($0.id)! < Int($1.id)!})
            /*
            var arrOfCategory: [String] = []
            for response in consent.response {
                if let category = response.qCategory, !arrOfCategory.contains(category) {
                    arrOfCategory.append(response.qCategory ?? "")
                }
            }
            
            for category in arrOfCategory {
                let categoryResponse = consent.response.filter({$0.qCategory == category})
                
                if categoryResponse.count == 1 {
                    for data in categoryResponse {
                        arrPlaceholder[1] = data.qCategory ?? EditConsentVCTableRow.faceDetection.title
                        arrDropDown1.append("Yes")
                        arrDropDown1.append("No")
                        arrSelectedID[1] = data.response == "true" ? 0 : 1
                    }
                } else {
                    for data in categoryResponse {
                        arrPlaceholder[0] = data.qCategory ?? EditConsentVCTableRow.datasetRelease.title
                        arrDropDown0.append(data.qCategoryResponse ?? "")
                        arrSelectedID[0] = data.response == "true" ? 1 : 0
                    }
                }
            }*/
            
            let datasetResponse = consent.response.filter({$0.qCategory == EditConsentVCTableRow.datasetRelease.key})
            
            for dataset in datasetResponse {
                arrPlaceholder[0] = dataset.qCategory ?? EditConsentVCTableRow.datasetRelease.title
                arrDropDown0.append(dataset.qCategoryResponse ?? "")
                arrSelectedID[0] = dataset.response == "true" ? 1 : 0
            }
            
            let faceRecognition = consent.response.filter({$0.qCategory == EditConsentVCTableRow.faceDetection.key})
            
            for face in faceRecognition {
                arrPlaceholder[1] = face.qCategory ?? EditConsentVCTableRow.faceDetection.title
                arrDropDown1.append("Yes")
                arrDropDown1.append("No")
                arrSelectedID[1] = face.response == "true" ? 0 : 1
            }
            
            let retention = consent.response.filter({$0.qCategory == EditConsentVCTableRow.retainmentTime.key})
            
            for retention in retention {
                arrPlaceholder[2] = retention.qCategory ?? EditConsentVCTableRow.retainmentTime.title
                arrDropDown2.append(retention.qCategoryResponse ?? "")
                arrSelectedID[2] = retention.response == "true" ? 1 : 0
            }
            
            self.editConsent = consent
        }
    }
}

// MARK:- API
extension EditConsentVC {
    
    private func requestForConsentResponse() {
        
        self.showProgress()
        ConsentAPI.verifySubject(subjectEmail: Collector.currentCollector.email) { [weak self] (result, error) in
            guard let self = self else { return }
            if let err = error {
                self.hideProgress()
                self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
            } else if let result = result {
                self.stringJsonDecode(str: result.consentResponse ?? "")
            } else {
                self.hideProgress()
            }
        }
    }
    
    private func updateConsentResponse() {
        
        let consentDetailsResponse = try! editConsent.asDictionary()
        
        let jsonData = try! JSONSerialization.data(withJSONObject: consentDetailsResponse, options: [])
        let consentDetailsResponseString = String(data: jsonData, encoding: .utf8)!
                
        let updateSubjectDevInput = UpdateStrSubjectInput(subjectEmail: Collector.currentCollector.email, collectorEmail: subjectCollectorEmail, consentResponse: "\(consentDetailsResponseString)")
        
        self.showProgress()
        
        ConsentAPI.updateEditConsentDetails(subjectInput: updateSubjectDevInput) { [weak self] (status, error) in
            guard let self = self else {return}
            self.hideProgress()
            if status {
                UIUtilities.showAlertMessageWithActionHandler(kUpdateCompleted, message: "", buttonTitle: "OK", viewControllerUsed: self) {
                    self.navigationController?.popViewController(animated: true)
                    
                }
            } else if let err = error {
                self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
            }
        }
        
    }
    
}
