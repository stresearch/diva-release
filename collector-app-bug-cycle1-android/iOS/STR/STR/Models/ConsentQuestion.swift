//
//  ConsentQuestion.swift
//  STR
//
//  Created by Srujan on 03/01/20.
//  
//

import Foundation

// MARK: - Consent
class Consent: Codable {
    let questions: [ConsentQuestion]
    let moreInfo: String

    init(questions: [ConsentQuestion], moreInfo: String) {
        self.questions = questions
        self.moreInfo = moreInfo
    }
    
    static func getConsentFromJSON() -> Consent? {
        if let consentJSONData = Utilities.getDataFromLocalJSON(filename: "Consent"),
            let consent = try? JSONDecoder().decode(Consent.self, from: consentJSONData) {
            return consent
        }
        return nil
    }
    
    static func getConsent(consentQuestionnaires: [ListStrConsentQuestionnairesQuery.Data.ListStrConsentQuestionnaire.Item]) -> Consent? {
        var questions: [ConsentQuestion] = []
        consentQuestionnaires.forEach { (result) in
            let question = ConsentQuestion(id: result.id, consentId: result.consentId ?? "" ,shortText: result.shortDescription ?? "", longText: result.longDescription ?? "" , agreeTargetID: result.agreeQuestionId ?? "", disagreeTargetID: result.disagreeQuestionId ?? "", category: result.category ?? "", category_response: result.categoryResponse ?? "", moreInfo: result.moreInfo ?? "")
            questions.append(question)
        }
        let consent = Consent(questions: questions, moreInfo: "")
        return consent
    }
    
    func getNextQuestion(currentQuestion: ConsentQuestion? = nil) -> ConsentQuestion? {
        
        if currentQuestion == nil {
            return self.questions.first
        } else {
            let nextQuestionID = (currentQuestion!.isAgreed) ? currentQuestion!.agreeTargetID : currentQuestion!.disagreeTargetID
            if let nextQuestion = self.questions.filter({$0.id == nextQuestionID}).first {
              return nextQuestion
            } else {
                return nil
            }
        }
    }
}

// MARK: - Question
class ConsentQuestion: Codable {
    let id: String
    let consent_id: String
    let short_description :String
    let long_description : String
    let agreeTargetID: String?
    let disagreeTargetID: String?
    let category: String
    let category_response: String
    let moreInfoUrl: String
    lazy var isAgreed = false
    init(id: String, consentId: String, shortText: String, longText: String, agreeTargetID: String, disagreeTargetID: String, category: String, category_response: String, moreInfo: String) {
        self.id = id
        self.short_description = shortText
        self.long_description = longText
        self.agreeTargetID = agreeTargetID
        self.disagreeTargetID = disagreeTargetID
        self.consent_id = consentId
        self.category = category
        self.category_response = category_response
        self.moreInfoUrl = moreInfo
    }
}

class ConsentResponse {
    
    var subjectEmail: String?
    var subjectID: String?
    var questionResponse: [JSONDictionary]?
    var consentS3UrlPath: String?
    var version: String?
    //var isSetUp: Bool = false
    var isEditConsent: Bool = false
    
    static let instance = ConsentResponse()
    private init() {}
    
    func clear() {
        subjectEmail = nil
        questionResponse = nil
        consentS3UrlPath = nil
        version = nil
    }
}


struct EditConsent: Codable {
    var response: [Response]
    
    static func getResponse(jsonObject: JSONDictionary) -> EditConsent? {
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: jsonObject, options: .prettyPrinted)
            let decoder = JSONDecoder()
            do {
                return try decoder.decode(EditConsent.self, from: jsonData)
            } catch {
                print(error.localizedDescription)
            }
        } catch {
            print(error.localizedDescription)
        }
        return nil
    }
}

// MARK: - Response
struct Response: Codable {
    let id: String
    let qCategory, qCategoryResponse: String?
    var response: String

    enum CodingKeys: String, CodingKey {
        case id
        case qCategory = "q_category"
        case qCategoryResponse = "q_category_response"
        case response
    }
}
