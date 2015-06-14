//
//  ConnViewController.swift
//  wifiworld
//
//  Created by Jane on 15/6/4.
//  Copyright (c) 2015年 com.anynet. All rights reserved.
//

import UIKit

class ConnViewController: UIViewController,UITableViewDataSource,UITableViewDelegate,MAMapViewDelegate {
    @IBOutlet weak var lb_CurrentWifi: UILabel!

    @IBOutlet weak var btn_CustomSwitch: UIButton!
   
    @IBOutlet weak var mp_tableView: UITableView!
    var mapView:MAMapView!
    
    @IBOutlet weak var btn_Search: UIButton!
    
    @IBOutlet weak var imV_search: UIImageView!
    
    
    var lockWifiList = [AnyObject]()
    var freeWifiList = [AnyObject]()
    var wifiInfo = [:]
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        self.initInterface();
    }

    override func viewWillAppear(animated: Bool) {
        mapView.tag = 0;
        
    }
    func initInterface(){
        self.navigationController?.navigationBar.setBackgroundImage(UIImage(named: "wifi_title_bg"), forBarMetrics: UIBarMetrics.Default)
        let statusView = UIView(frame: CGRectMake(0, -20, UIScreen.mainScreen().bounds.width, 20));
        statusView.backgroundColor = UIColor.whiteColor();
        self.navigationController?.navigationBar.addSubview(statusView);
        UIApplication.sharedApplication().setStatusBarStyle(UIStatusBarStyle.Default, animated: true);
        if let infoDic = CurrentWifiSSID.fetchSSIDInfo() as? NSDictionary{
            wifiInfo = infoDic;
        }
        self.btn_CustomSwitch.addTarget(self, action: "clickLeftCustomSwitch:", forControlEvents: UIControlEvents.TouchUpInside)
        let wifiName = wifiInfo["SSID"] as? String;
        self.lb_CurrentWifi.text = wifiName;
        self.mapView = MAMapView(frame: CGRectZero);
        self.mapView.delegate = self;
        self.mapView.showsUserLocation = true;
        self.btn_Search.addTarget(self, action: "clickSearch", forControlEvents: UIControlEvents.TouchUpInside);
        
    }
    
    func clickSearch(){
        UIView.animateWithDuration(1.0,
            delay: 0.0,
            options: .CurveEaseInOut | .AllowUserInteraction,
            animations: {
                self.imV_search.transform = CGAffineTransformMakeRotation(CGFloat(M_PI))
            },
            completion: { finished in
                println("Bug faced right!")
        })
    }
    
    func clickLeftCustomSwitch(button:UIButton!){
    
        if button.tag == 0{
            button.setBackgroundImage(UIImage(named: "wifi_switch_on"), forState: UIControlState.Normal);
            button.tag = 1;
        }else {
            button.tag = 0;
            button.setBackgroundImage(UIImage(named: "wifi_switch_off"), forState: UIControlState.Normal);
        }
    
    }
    
    func queryObject(loc:CLLocation!){
        let wifiProfile = WifiProfile();
        let geo = BmobGeoPoint(longitude: loc.coordinate.longitude , withLatitude: loc.coordinate.latitude );
        wifiProfile.queryObject(geo, kilometers: 0.10){ [weak self](list,eror) ->Void in
            //println("dataList=\(list)");
                if list != nil {
                    self!.freeWifiList = list!;
                    self?.mp_tableView.reloadData();
                    println("dataList=\(list?.count)");
                    NSLog("%ld", list!.count);
                }else {
                    //self?.mapView.tag = 0;
                }
        };
    }
    //MARK: - mapviewDlegate
    
    func mapView(mapView: MAMapView!, didUpdateUserLocation userLocation: MAUserLocation!, updatingLocation: Bool) {
        NSLog("%f", userLocation.coordinate.latitude);

        println("\(userLocation.coordinate.latitude)");
        if updatingLocation && mapView.tag == 0 && userLocation.location.coordinate.latitude != 0{
            mapView.tag = 1;
            self.queryObject(userLocation.location)
            //NSThread.detachNewThreadSelector("queryObject:", toTarget: self, withObject: userLocation.location)
        }
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */
    //MARK: - tableviewDelegate
    
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 2;
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if section == 0{
            return self.freeWifiList.count;
        }else{
            return self.lockWifiList.count;
        }
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell{
        
        var cellIdentifier = "CustomCell";
        var bkgName = "";
        var wifiname = "";
        let obj = freeWifiList[indexPath.row] as? BmobObject ;

        if indexPath.section == 0{
            if indexPath.row == 0{
                bkgName = "wifi_free_item0";
            }else if indexPath.row == (self.freeWifiList.count - 1){
                bkgName = "wifi_free_item2";
            }else {
                bkgName = "wifi_free_item1";
            }
            
            if let name = obj?.objectForKey("Alias") as? String  {
                wifiname = name;
            }else {
                println("not dictionary");

            }
//            if alias != nil{
//                wifiname = alias!
//            }
            
        }else{
            if indexPath.row == 0{
                bkgName = "wifi_lock_item0";
            }else if indexPath.row == (self.freeWifiList.count - 1){
                bkgName = "wifi_lock_item2";
            }else {
                bkgName = "wifi_lock_item1";
            }
        }
        let cell = tableView.dequeueReusableCellWithIdentifier(cellIdentifier, forIndexPath: indexPath) as! CustomConnectCell;
        let bkgV = UIImageView(image: UIImage(named: bkgName));
        bkgV.frame = CGRectMake(0, 0, tableView.frame.width, cell.frame.height);
        cell.backgroundView = bkgV;
        cell.lb_wifiName?.text = "\(wifiname)";
        cell.imgV_wifiheadImage?.image = UIImage(named: "wifi_free_signal3")
        let btn_Accessory = UIButton(frame: CGRectMake(30, 0, 40, 40));
        btn_Accessory.setImage(UIImage(named: "1_1_86"), forState: UIControlState.Normal);
        btn_Accessory.addTarget(self, action: "tapAccessory", forControlEvents: UIControlEvents.TouchUpInside);
        cell.accessoryView = btn_Accessory;
        if let id = obj?.objectForKey("Ssid") as? String  {
            cell.lb_wifiAddress.text = id;
        }
        return cell;
    }
    
    func tapAccessory(){
        self.performSegueWithIdentifier("wifiDetail", sender: nil);
    }
    
    
    
    func tableView(tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        
        let view  = UIView(frame: CGRectMake(0, 0, 100, 30));
        //view.backgroundColor = UIColor.lightTextColor();
        var imgName = "";
        var rect = CGRectZero;
        let label = UILabel(frame: CGRectMake(20, 0, 100, 20));
        label.textColor = UIColor.grayColor();
        if section == 0{
            label.text = "认证Wi-Fi"
            imgName = "wifi_free_title_icon";
            rect = CGRectMake(0, 0, 10, 20);
        }else{
            imgName = "wifi_lock_title_icon"
            label.text = "非认证Wi-Fi"
            rect = CGRectMake(0, 0, 15, 20);
        };
        let imgV = UIImageView(image: UIImage(named: imgName));
        imgV.frame = rect;
        view.addSubview(imgV);
        view.addSubview(label);
        return view;
    }

    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        tableView.deselectRowAtIndexPath(indexPath, animated: true);
    }
    
}

class CustomConnectCell: UITableViewCell {
    @IBOutlet weak var lb_wifiName: UILabel!
   
    @IBOutlet weak var lb_passport: UILabel!
    
    @IBOutlet weak var lb_wifiAddress: UILabel!
    
    @IBOutlet weak var imgV_wifiheadImage: UIImageView!
    
    
}
