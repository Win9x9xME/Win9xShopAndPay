import requests
import json
import time
import random
def ot(text):
    for i in text:
        print(i,end="")
        time.sleep(random.randint(1,20)/100)
    time.sleep(0.5)
while True:
    a=input("""
user:""")
    print("ai:加载中……")
    url="https://api.sizhi.com/bot?appid=9ffcb5785ad9617bf4e64178ac64f7b1&spoken="+a
    response = requests.get(url)
    data = json.loads(response.text)
    whether = str(data["status"])
    message = str(data["message"])
    if whether == "0":
        value = data["data"]["info"]["text"]
        print("ai:",end="")
        ot(value)
    else:
        ot("出错啦！错误码:"+whether+" 原因:"+message)