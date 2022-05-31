//
//  Credentials.swift
//  STR
//
//  Created by Srujan on 10/06/19.
//  Copyright © 2019 BTC. All rights reserved.

import CoreData

class Credentials: NSManagedObject, Codable {

    // MARK: - Properties
    @NSManaged var accessToken: String!

    @NSManaged var refreshToken: String!

    /**
    The token for the request header.

        Token depends on it's type:

            let basic = Basic  <combination of app id and some key and encoded into base64>
            let bearer = Bearer <accessToken>
    */
    var authorizationToken: String {
        return
            "Basic Y29tLmJ0Yy5TYWZlUGFzc2FnZURyaXZlcjpremFMWGxWckNvZU1BQTR2SGdQSGxJOU1FUmx4d2hIVQ=="  // TBD: need to get it using bundle identifier and username
    }

    // MARK: - Enums and Structs
    private struct JSONKey {
        static let accessToken = "accessToken"
        static let refreshToken = "refreshToken"
        static let tokenType = "tokenType"
    }

    // MARK: - Initializers

    required convenience init(from decoder: Decoder) throws {
        let managedObjectContext = UserDiskService.persistentContainer.viewContext

        guard
            let entity = NSEntityDescription.entity(
                forEntityName: "Credentials",
                in: managedObjectContext
            )
        else { fatalError("Failed to decode Credentials") }
        self.init(entity: entity, insertInto: managedObjectContext)
        
        accessToken = try decoder.decode(JSONKey.accessToken)
        refreshToken = try decoder.decode(JSONKey.refreshToken)

    }

    /**
    Creates a new instance with all the properties value.
     
     - Parameter accessToken: Auth token for the current logged in user
     - Parameter refreshToken: Token to refresh the accessToken after it's expired
     - Parameter tokenType: Type of Auth, Basic or Bearer
     
     */
    convenience init(accessToken: String, refreshToken: String, tokenType: String) {
        
        let managedObjectContext = UserDiskService.persistentContainer.viewContext

        guard
            let entity = NSEntityDescription.entity(
                forEntityName: "Credentials",
                in: managedObjectContext
            )
        else { fatalError("Failed to decode Credentials") }
        self.init(entity: entity, insertInto: managedObjectContext)
        
        self.accessToken = accessToken
        self.refreshToken = refreshToken

    }

}
