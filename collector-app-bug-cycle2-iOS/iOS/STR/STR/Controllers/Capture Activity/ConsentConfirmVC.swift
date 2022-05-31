//
//  ConsentConfirmVC.swift
//  STR
//
//  Created by Srujan on 02/01/20.
//  
//

import UIKit

class ConsentConfirmVC: UIViewController {
    
    @IBOutlet weak var closeBtn: UIButton!
    private var consent: Consent?
    private var currentQuestionView: ConsentQuestionView?
    var arrOfQuestionResponse: [JSONDictionary]?
    var moreInfoUrl: String?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        arrOfQuestionResponse = []
        self.requestForConsentQuestionnaire() //TBD
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        updateNavigationBar()
        
    }
    
    // MARK:- UI Utils
    
    private func updateNavigationBar() {
        self.navigationController?.navigationBar.isHidden = true
        self.title = LocalizableString.consent.localizedString.uppercased()
    }
    
    private func askQuestion(question: ConsentQuestion) {
        
        guard let consentQuestionView = ConsentQuestionView.instanceFromNib()
            else {return}
        consentQuestionView.delegate = self
        consentQuestionView.configureView(with: question)
        self.currentQuestionView = consentQuestionView
        self.moreInfoUrl = question.moreInfoUrl
        consentQuestionView.isHidden = true
        self.view.addSubview(consentQuestionView)
        consentQuestionView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            consentQuestionView.centerYAnchor.constraint(equalTo: self.view.centerYAnchor),
            consentQuestionView.heightAnchor.constraint(equalToConstant: 200),
            consentQuestionView.leadingAnchor.constraint(equalTo: self.view.leadingAnchor),
            consentQuestionView.trailingAnchor.constraint(equalTo: self.view.trailingAnchor)
        ])
        self.view.layoutIfNeeded()
        UIView.animate(withDuration: 0.3, delay: 1, options: [.transitionFlipFromLeft], animations: { [weak self] in
            consentQuestionView.isHidden = false
            self?.view.layoutIfNeeded()
            }, completion: nil)
    }
    
    
    private func didShowRecordConsentVC() {
        
        consent?.questions.forEach({ (result) in
            if !(arrOfQuestionResponse?.contains(where: {($0["id"] as? String) == result.id}) ?? false) {
                
                if result.id == "6" || result.id == "7" {
                    let json = ["id": result.id, "response": "false"]
                    arrOfQuestionResponse?.append(json)
                } else {
                    let json = ["id": result.id,"q_category": result.category, "q_category_response": result.category_response ,"response": "false"]
                    arrOfQuestionResponse?.append(json)
                }
            }
        })
        arrOfQuestionResponse?.sort(by: {Int(($0["id"] as? String)!)! < Int(($1["id"] as? String)!)!})
        
        ConsentResponse.instance.questionResponse = arrOfQuestionResponse
        let recordConsentVC: RecordConsentVC? = self.storyboard?.instantiateViewController()
        if recordConsentVC != nil {
            ConsentResponse.instance.isEditConsent = false
            self.navigationController?.pushViewController(recordConsentVC!, animated: true)
        }
    }
    
    
    // MARK:- Actions
    @IBAction func closeButtonAction(_ sender: UIButton) {
        self.showAlertWithDecision(title: "Close", message: kExitCollection, successHandler: { [weak self] (_) in
            self?.navigationController?.popToRootViewController(animated: true)
            }, completion: nil)
    }
    
    @IBAction func backBtnPressed(_ sender: UIButton) {
        self.navigationController?.popViewController(animated: true)
    }
    
    @IBAction func moreInfoBtnPressed(_ sender: UIButton) {
       let storyboard = UIStoryboard(name: "Activity", bundle: nil)
        let webVC: WebViewController? = storyboard.instantiateViewController()
        if webVC != nil {
            webVC!.moreInfo = self.moreInfoUrl ?? ""
            webVC!.webViewType = .html
            self.present(webVC!, animated: true, completion: nil)
        }
    }
    
}

extension ConsentConfirmVC: ConsentQuestionViewDelegate {

    func didAgree(for question: ConsentQuestion?) {
        
        if let agreeID = question?.agreeTargetID,
            agreeID == "0" {
            self.didShowRecordConsentVC()
            return
        }
        
        if let question = question {
            let json = ["id": question.id,"q_category": question.category, "q_category_response": question.category_response ,"response": "true"]
            arrOfQuestionResponse?.append(json)
            updateNextQuestion(currentQuestion: question)
        }
    }
    
    func didDisAgree(for question: ConsentQuestion?) {
        if let disAgreeID = question?.disagreeTargetID,
            disAgreeID == "0" {
            self.navigationController?.popViewController(animated: true)
            return
        }
        if let question = question {
            let json = ["id": question.id,"q_category": question.category, "q_category_response": question.category_response ,"response": "false"]
            arrOfQuestionResponse?.append(json)
            updateNextQuestion(currentQuestion: question)
        }
    }
    
    fileprivate func updateNextQuestion(currentQuestion: ConsentQuestion) {
        if let currentQuestionView = self.currentQuestionView {
            UIView.animate(withDuration: 0.3, delay: 0, options: [.curveEaseOut], animations: { [weak self] in
                guard let self = self else {return}
                currentQuestionView.removeFromSuperview()
                self.view.layoutIfNeeded()
                }, completion: { (finished) in
                    if let question = self.consent?.getNextQuestion(currentQuestion: currentQuestion){
                        self.askQuestion(question: question)
                    } else {
                        self.didShowRecordConsentVC()
                    }
            })
        }
    }
}

//MARK: Api Call
extension ConsentConfirmVC {
    
    private func requestForConsentQuestionnaire() {
        self.showProgress()
        ConsentAPI.getConsentQuestionnaire { (result, error) in
            self.hideProgress()
            if let error = error {
                self.presentDefaultAlertWithError(error: error, animated: true, completion: nil)
            } else {
                if let consent = Consent.getConsent(consentQuestionnaires: result ?? []),
                    let question = consent.getNextQuestion() {
                    self.consent = consent
                    self.askQuestion(question: question)
                }
            }
        }
    }
}
