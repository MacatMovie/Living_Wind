#!/usr/bin/env python3
"""Illustrative dry/clear, unobstructed Overworld distribution. No Minecraft dependencies.
Approximation of WindSavedData update logic sampled every 20 game ticks.
Usage: python scripts/simulate-wind-distribution.py
"""
import math
import random

BUCKETS = (0.1, 0.3, 0.5, 0.8, 1.2)


def sample(seed, days=140, tuned=False, params=None):
    r = random.Random(seed)
    params = params or {}
    def p(key, default): return params.get(key, default)
    minn = p('min', 0.04 if tuned else 0.08)
    maxx = p('max', 0.48)
    windy_prob = p('windy_prob', .55 if tuned else .38)
    dur_min = p('dur_min', 3000 if tuned else 2400)
    dur_max = p('dur_max', 7200 if tuned else 6000)
    calm_prob = p('calm_prob', .09 if tuned else 0)
    breezy_prob = p('breezy_prob', .26 if tuned else 0)
    calm_min = p('calm_min', .17)
    calm_max = p('calm_max', .33)
    breeze_min = p('breeze_min', .30)
    breeze_max = p('breeze_max', .40)
    windy_min = p('windy_min', .28 if tuned else .16)
    windy_max = p('windy_max', .48 if tuned else .46)
    base, target, turbulence = .12, .18, .28
    next_target, next_gust = 0, 0
    gust_start, gust_end, gust_mag = -1, -1, 0
    phase_a, phase_b = r.random()*2*math.pi, r.random()*2*math.pi
    counts = [0]*6
    for day in range(days):
        day_offset = (r.random()-r.random())*.045
        calm_day = False
        if tuned:
            regime = r.random()
            if regime < calm_prob:
                calm_day = True
                day_offset -= calm_min + r.random()*(calm_max-calm_min)
            elif regime < calm_prob+breezy_prob:
                day_offset += breeze_min + r.random()*(breeze_max-breeze_min)
            elif r.random() < .12:
                day_offset += .08 + r.random()*.13
        elif r.random() < .12:
            day_offset += .08 + r.random()*.13
        nr=r.random()
        night_mult = (.50+r.random()*.32 if nr < .72 else (.86+r.random()*.30 if nr < .94 else 1.20+r.random()*.45))
        windy=r.random()<windy_prob and not calm_day
        if windy:
            duration=r.randint(dur_min,dur_max)
            start=r.randrange(0,24000-duration+1) if tuned else r.randrange(24000)
            boost=windy_min+r.random()*(windy_max-windy_min)
            if r.random()<.09: boost+=.15+r.random()*.16
        for day_tick in range(0,24000,20):
            now=day*24000+day_tick
            on = windy and ((start <= day_tick < start+duration) if tuned else (((start+duration)%24000 >= start and start <= day_tick < (start+duration)%24000) or ((start+duration)%24000 < start and (day_tick >= start or day_tick < (start+duration)%24000))))
            if now >= next_target:
                target=minn+((r.random()+r.random())*.5)*(maxx-minn)
                target_turb=.16+r.random()*.34+(.10 if on else 0)
                # Approximate target selection and daily turbulence tracking.
                turbulence += max(-.018,min(.018,target_turb-turbulence))
                delay = max(120,int((260+r.randrange(501))*(1.10-turbulence*.45)))
                next_target=now+delay
            if now >= next_gust and now >= gust_end:
                chance=.07+turbulence*.16+(.12 if on else 0)
                if r.random() < chance:
                    duration_g=90+r.randrange(131)
                    gust_mag=.07+r.random()*(.15+turbulence*.23)
                    if r.random()<.035: gust_mag*=1.60
                    gust_start, gust_end=now, now+duration_g
                next_gust=now+100+r.randrange(181)
            base += max(-20*(.0008+turbulence*.0012), min(20*(.0008+turbulence*.0012),target-base))
            if day_tick<12000: nightf=0
            elif day_tick<14000: nightf=(day_tick-12000)/2000
            elif day_tick<22000: nightf=1
            else: nightf=1-(day_tick-22000)/2000
            variation=math.sin(now*.037+phase_a)*(.018+turbulence*.03)+math.sin(now*.011+phase_b)*(.025+turbulence*.045)
            gust=(gust_mag*math.sin(math.pi*(now-gust_start)/(gust_end-gust_start)) if gust_start<=now<gust_end else 0)
            value=max(0, min(1.6, max(0,base+day_offset)*(1+(night_mult-1)*nightf)+(boost if on else 0)+variation+gust))*1.3
            i=sum(value>=boundary for boundary in BUCKETS)
            counts[i]+=1
    return [round(100*n/sum(counts),2) for n in counts]

if __name__ == '__main__':
    seeds = (101, 1033, 4321, 99991)
    for tune in (False, True):
        runs = [sample(seed, 140, tune) for seed in seeds]
        averages = [round(sum(run[i] for run in runs) / len(runs), 2) for i in range(6)]
        print(('v0.2.5' if tune else 'v0.2.4'), averages, 'percent, dry exposed conditions')
