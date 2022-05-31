
def time_format(dt):
    return "%s:%.6f%s" % (
        dt.strftime('%Y-%m-%dT%H:%M'),
        float("%.6f" % (dt.second + dt.microsecond / 1e6)),
        dt.strftime('%z')
    )   